package com.quertimizer.admin.application.service;

import com.quertimizer.global.constant.UserRole;
import com.quertimizer.admin.presentation.dto.request.AuthManageProblemPermissionUpdateReq;
import com.quertimizer.admin.presentation.dto.request.AuthManageRoleUpdateReq;
import com.quertimizer.admin.presentation.dto.response.AuthManageRes;
import com.quertimizer.admin.presentation.dto.response.AuthManageUserRowRes;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.domain.entity.ProblemGeneratorPermission;
import com.quertimizer.problem.domain.entity.ProblemSet;
import com.quertimizer.user.domain.entity.User;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.problem.infrastructure.repository.ProblemGeneratorPermissionRepository;
import com.quertimizer.problem.infrastructure.repository.ProblemRepository;
import com.quertimizer.problem.infrastructure.repository.ProblemSetRepository;
import com.quertimizer.user.infrastructure.repository.UserRepository;
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

import static com.quertimizer.admin.domain.model.AuthManageFailReason.INVALID_PERMISSION_KEY;
import static com.quertimizer.admin.domain.model.AuthManageFailReason.INVALID_ROLE;
import static com.quertimizer.admin.domain.model.AuthManageFailReason.LAST_ADMIN_PROTECTION;
import static com.quertimizer.admin.domain.model.AuthManageFailReason.PROBLEM_GENERATOR_REQUIRED;
import static com.quertimizer.admin.domain.model.AuthManageFailReason.USER_NOT_FOUND;
import static com.quertimizer.problem.domain.model.ProblemPermissionKey.NEW;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthManageService {

    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final ProblemSetRepository problemSetRepository;
    private final ProblemGeneratorPermissionRepository problemGeneratorPermissionRepository;

    public AuthManageRes getAuthManage() {
        List<User> users = userRepository.findAllByOrderByHandleAsc();
        Map<String, List<String>> permissionMap = problemGeneratorPermissionRepository.findAllByOrderByIdHandleAscIdProblemIdAsc().stream()
                .collect(Collectors.groupingBy(
                        ProblemGeneratorPermission::getHandle,
                        Collectors.mapping(permission -> normalizeStoredPermissionKey(permission.getProblemId()), Collectors.toList())
                ));

        List<AuthManageUserRowRes> members = users.stream()
                .filter(User::hasHandle)
                .map(user -> new AuthManageUserRowRes(
                        user.getHandle(),
                        resolveRoleValue(user.getResolvedRole()),
                        user.getResolvedRole() == UserRole.PROBLEM_GENERATOR
                                ? sortPermissionKeys(permissionMap.getOrDefault(user.getHandle(), List.of()))
                                : List.of()
                ))
                .toList();

        return new AuthManageRes(members);
    }

    @Transactional
    public void updateUserRole(String handle, AuthManageRoleUpdateReq request) {
        User user = findUser(handle);
        UserRole nextRole = normalizeRole(request.getRole());

        if (user.getResolvedRole() == UserRole.ADMIN && nextRole != UserRole.ADMIN) {
            validateAdminRoleRemoval();
        }

        user.changeRole(nextRole);

        if (nextRole != UserRole.PROBLEM_GENERATOR) {
            problemGeneratorPermissionRepository.deleteAllByIdHandle(handle);
        }
    }

    @Transactional
    public void updateProblemGeneratorPermissions(String handle, AuthManageProblemPermissionUpdateReq request) {
        User user = findUser(handle);

        if (user.getResolvedRole() != UserRole.PROBLEM_GENERATOR) {
            throw new BusinessException(PROBLEM_GENERATOR_REQUIRED.getMessage(), HttpStatus.BAD_REQUEST);
        }

        List<String> permissionKeys = normalizePermissionKeys(request.getPermissionKeys());
        validatePermissionKeys(permissionKeys);

        problemGeneratorPermissionRepository.deleteAllByIdHandle(handle);

        if (!permissionKeys.isEmpty()) {
            problemGeneratorPermissionRepository.saveAll(permissionKeys.stream()
                    .map(permissionKey -> ProblemGeneratorPermission.create(handle, permissionKey))
                    .toList());
        }
    }

    private User findUser(String handle) {
        return userRepository.findByHandle(handle)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
    }

    private UserRole normalizeRole(String role) {
        String normalizedRole = Optional.ofNullable(role)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.replace("_", "").replace("-", "").toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new BusinessException(INVALID_ROLE.getMessage(), HttpStatus.BAD_REQUEST));

        return switch (normalizedRole) {
            case "ADMIN" -> UserRole.ADMIN;
            case "USER" -> UserRole.USER;
            case "PROBLEMGENERATOR" -> UserRole.PROBLEM_GENERATOR;
            default -> throw new BusinessException(INVALID_ROLE.getMessage(), HttpStatus.BAD_REQUEST);
        };
    }

    private void validateAdminRoleRemoval() {
        long adminCount = userRepository.findAllByOrderByHandleAsc().stream()
                .filter(user -> user.getResolvedRole() == UserRole.ADMIN)
                .count();

        if (adminCount <= 1) {
            throw new BusinessException(LAST_ADMIN_PROTECTION.getMessage(), HttpStatus.BAD_REQUEST);
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
                !NEW.getValue().equals(permissionKey)
                        && !existingProblemIds.contains(permissionKey)
                        && !existingProblemSetIds.contains(permissionKey))) {
            throw new BusinessException(INVALID_PERMISSION_KEY.getMessage(), HttpStatus.BAD_REQUEST);
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

        if (NEW.getValue().equals(normalizedPermissionKey)) {
            return NEW.getValue();
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
        if (NEW.getValue().equals(permissionKey)) {
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
