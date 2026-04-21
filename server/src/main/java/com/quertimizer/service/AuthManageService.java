package com.quertimizer.service;

import com.quertimizer.constant.UserRole;
import com.quertimizer.endpoint.api.dto.request.AuthManageProblemPermissionUpdateReq;
import com.quertimizer.endpoint.api.dto.request.AuthManageRoleUpdateReq;
import com.quertimizer.endpoint.api.dto.response.AuthManageRes;
import com.quertimizer.endpoint.api.dto.response.AuthManageUserRowRes;
import com.quertimizer.entity.Problem;
import com.quertimizer.entity.ProblemGeneratorPermission;
import com.quertimizer.entity.ProblemSet;
import com.quertimizer.entity.User;
import com.quertimizer.exception.BusinessException;
import com.quertimizer.repository.ProblemGeneratorPermissionRepository;
import com.quertimizer.repository.ProblemRepository;
import com.quertimizer.repository.ProblemSetRepository;
import com.quertimizer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthManageService {

    private static final String USER_NOT_FOUND_MESSAGE = "존재하지 않는 사용자다.";
    private static final String INVALID_ROLE_MESSAGE = "지원하지 않는 역할이다.";
    private static final String LAST_ADMIN_PROTECTION_MESSAGE = "마지막 Admin 역할은 해제할 수 없다.";
    private static final String PROBLEM_GENERATOR_REQUIRED_MESSAGE = "ProblemGenerator만 문제 권한을 수정할 수 있다.";
    private static final String INVALID_PERMISSION_KEY_MESSAGE = "존재하지 않는 문제 또는 테이블셋 권한이 포함되어 있다.";
    private static final String NEW_PERMISSION_KEY = "NEW";

    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final ProblemSetRepository problemSetRepository;
    private final ProblemGeneratorPermissionRepository problemGeneratorPermissionRepository;

    public AuthManageRes getAuthManage() {
        List<User> users = userRepository.findAllByOrderByUserIdAsc();
        Map<String, List<String>> permissionMap = problemGeneratorPermissionRepository.findAllByOrderByIdUserIdAscIdProblemIdAsc().stream()
                .collect(Collectors.groupingBy(
                        ProblemGeneratorPermission::getUserId,
                        Collectors.mapping(permission -> normalizeStoredPermissionKey(permission.getProblemId()), Collectors.toList())
                ));

        List<AuthManageUserRowRes> members = users.stream()
                .filter(User::hasUserId)
                .map(user -> new AuthManageUserRowRes(
                        user.getUserId(),
                        resolveRoleValue(user.getResolvedRole()),
                        user.getResolvedRole() == UserRole.PROBLEM_GENERATOR
                                ? sortPermissionKeys(permissionMap.getOrDefault(user.getUserId(), List.of()))
                                : List.of()
                ))
                .toList();

        return new AuthManageRes(members);
    }

    @Transactional
    public void updateUserRole(String userId, AuthManageRoleUpdateReq request) {
        User user = findUser(userId);
        UserRole nextRole = normalizeRole(request.getRole());

        if (user.getResolvedRole() == UserRole.ADMIN && nextRole != UserRole.ADMIN) {
            validateAdminRoleRemoval();
        }

        user.changeRole(nextRole);

        if (nextRole != UserRole.PROBLEM_GENERATOR) {
            problemGeneratorPermissionRepository.deleteAllByIdUserId(userId);
        }
    }

    @Transactional
    public void updateProblemGeneratorPermissions(String userId, AuthManageProblemPermissionUpdateReq request) {
        User user = findUser(userId);

        if (user.getResolvedRole() != UserRole.PROBLEM_GENERATOR) {
            throw new BusinessException(PROBLEM_GENERATOR_REQUIRED_MESSAGE, HttpStatus.BAD_REQUEST);
        }

        List<String> permissionKeys = normalizePermissionKeys(request.getPermissionKeys());
        validatePermissionKeys(permissionKeys);

        problemGeneratorPermissionRepository.deleteAllByIdUserId(userId);

        if (!permissionKeys.isEmpty()) {
            problemGeneratorPermissionRepository.saveAll(permissionKeys.stream()
                    .map(permissionKey -> ProblemGeneratorPermission.create(userId, permissionKey))
                    .toList());
        }
    }

    private User findUser(String userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND_MESSAGE, HttpStatus.NOT_FOUND));
    }

    private UserRole normalizeRole(String role) {
        String normalizedRole = Optional.ofNullable(role)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.replace("_", "").replace("-", "").toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BusinessException(INVALID_ROLE_MESSAGE, HttpStatus.BAD_REQUEST));

        return switch (normalizedRole) {
            case "ADMIN" -> UserRole.ADMIN;
            case "USER" -> UserRole.USER;
            case "PROBLEMGENERATOR" -> UserRole.PROBLEM_GENERATOR;
            default -> throw new BusinessException(INVALID_ROLE_MESSAGE, HttpStatus.BAD_REQUEST);
        };
    }

    private void validateAdminRoleRemoval() {
        long adminCount = userRepository.findAllByOrderByUserIdAsc().stream()
                .filter(user -> user.getResolvedRole() == UserRole.ADMIN)
                .count();

        if (adminCount <= 1) {
            throw new BusinessException(LAST_ADMIN_PROTECTION_MESSAGE, HttpStatus.BAD_REQUEST);
        }
    }

    private List<String> normalizePermissionKeys(List<String> permissionKeys) {
        return Optional.ofNullable(permissionKeys)
                .orElse(List.of())
                .stream()
                .map(this::normalizePermissionKey)
                .filter(permissionKey -> !permissionKey.isEmpty())
                .distinct()
                .toList();
    }

    private void validatePermissionKeys(List<String> permissionKeys) {
        if (permissionKeys.isEmpty()) {
            return;
        }

        Set<String> problemIds = permissionKeys.stream()
                .filter(this::isScopedProblemId)
                .collect(Collectors.toSet());
        Set<String> problemSetIds = permissionKeys.stream()
                .filter(this::isScopedProblemSetId)
                .collect(Collectors.toSet());
        Set<String> existingProblemIds = StreamSupport.stream(problemRepository.findAllById(problemIds).spliterator(), false)
                .map(Problem::getProblemId)
                .collect(Collectors.toSet());
        Set<String> existingProblemSetIds = StreamSupport.stream(problemSetRepository.findAllById(problemSetIds).spliterator(), false)
                .map(ProblemSet::getProblemSetId)
                .collect(Collectors.toSet());

        if (permissionKeys.stream().anyMatch(permissionKey ->
                !NEW_PERMISSION_KEY.equals(permissionKey)
                        && !existingProblemIds.contains(permissionKey)
                        && !existingProblemSetIds.contains(permissionKey))) {
            throw new BusinessException(INVALID_PERMISSION_KEY_MESSAGE, HttpStatus.BAD_REQUEST);
        }
    }

    private String normalizePermissionKey(String permissionKey) {
        String normalizedPermissionKey = Optional.ofNullable(permissionKey)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toUpperCase(Locale.ROOT))
                .orElse("");

        if (normalizedPermissionKey.isEmpty()) {
            return "";
        }

        if (NEW_PERMISSION_KEY.equals(normalizedPermissionKey)) {
            return NEW_PERMISSION_KEY;
        }

        if (normalizedPermissionKey.matches("^\\d{5}-\\d{5}$")) {
            return "P" + normalizedPermissionKey;
        }

        if (normalizedPermissionKey.matches("^\\d{5}$")) {
            return "P" + normalizedPermissionKey;
        }

        return normalizedPermissionKey;
    }

    private String normalizeStoredPermissionKey(String permissionKey) {
        String normalizedPermissionKey = normalizePermissionKey(permissionKey);
        return normalizedPermissionKey.isEmpty()
                ? Optional.ofNullable(permissionKey).map(String::trim).orElse("")
                : normalizedPermissionKey;
    }

    private boolean isScopedProblemId(String permissionKey) {
        return permissionKey.matches("^[PO]\\d{5}-\\d{5}$");
    }

    private boolean isScopedProblemSetId(String permissionKey) {
        return permissionKey.matches("^[PO]\\d{5}$");
    }

    private List<String> sortPermissionKeys(List<String> permissionKeys) {
        return permissionKeys.stream()
                .filter(permissionKey -> !permissionKey.isBlank())
                .distinct()
                .sorted(Comparator.comparingInt(this::resolvePermissionOrder).thenComparing(String::compareTo))
                .toList();
    }

    private int resolvePermissionOrder(String permissionKey) {
        if (NEW_PERMISSION_KEY.equals(permissionKey)) {
            return 0;
        }

        if (isScopedProblemSetId(permissionKey)) {
            return 1;
        }

        if (isScopedProblemId(permissionKey)) {
            return 2;
        }

        return 3;
    }

    private String resolveRoleValue(UserRole role) {
        return switch (role) {
            case ADMIN -> "admin";
            case PROBLEM_GENERATOR -> "problemGenerator";
            default -> "user";
        };
    }

}
