package com.quertimizer.service;

import com.quertimizer.constant.UserRole;
import com.quertimizer.endpoint.api.dto.request.AuthManageProblemPermissionUpdateReq;
import com.quertimizer.endpoint.api.dto.request.AuthManageRoleUpdateReq;
import com.quertimizer.endpoint.api.dto.response.AuthManageMemberRes;
import com.quertimizer.endpoint.api.dto.response.AuthManageProblemGeneratorGroupRes;
import com.quertimizer.endpoint.api.dto.response.AuthManageProblemGeneratorMemberRes;
import com.quertimizer.endpoint.api.dto.response.AuthManageRes;
import com.quertimizer.endpoint.api.dto.response.AuthManageRoleGroupRes;
import com.quertimizer.entity.ProblemGeneratorPermission;
import com.quertimizer.entity.Problem;
import com.quertimizer.entity.User;
import com.quertimizer.exception.BusinessException;
import com.quertimizer.repository.ProblemRepository;
import com.quertimizer.repository.ProblemGeneratorPermissionRepository;
import com.quertimizer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private static final String INVALID_PROBLEM_ID_MESSAGE = "존재하지 않는 문제 ID가 포함되어 있다.";

    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final ProblemGeneratorPermissionRepository problemGeneratorPermissionRepository;

    public AuthManageRes getAuthManage() {
        List<User> users = userRepository.findAllByOrderByUserIdAsc();
        List<ProblemGeneratorPermission> problemGeneratorPermissions =
                problemGeneratorPermissionRepository.findAllByOrderByIdUserIdAscIdProblemIdAsc();

        return new AuthManageRes(
                createRoleGroup(users, UserRole.ADMIN),
                createRoleGroup(users, UserRole.USER),
                createProblemGeneratorGroup(users, problemGeneratorPermissions)
        );
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

        List<String> problemIds = normalizeProblemIds(request.getProblemIds());
        validateProblemIds(problemIds);

        problemGeneratorPermissionRepository.deleteAllByIdUserId(userId);

        if (!problemIds.isEmpty()) {
            problemGeneratorPermissionRepository.saveAll(problemIds.stream()
                    .map(problemId -> ProblemGeneratorPermission.create(userId, problemId))
                    .toList());
        }
    }

    private AuthManageRoleGroupRes createRoleGroup(List<User> users, UserRole role) {
        List<AuthManageMemberRes> members = users.stream()
                .filter(User::hasUserId)
                .filter(user -> user.getResolvedRole() == role)
                .map(AuthManageMemberRes::from)
                .toList();

        return new AuthManageRoleGroupRes(members.size(), members);
    }

    private AuthManageProblemGeneratorGroupRes createProblemGeneratorGroup(List<User> users,
                                                                           List<ProblemGeneratorPermission> problemGeneratorPermissions) {
        Map<String, List<String>> permissionMap = problemGeneratorPermissions.stream()
                .collect(Collectors.groupingBy(
                        ProblemGeneratorPermission::getUserId,
                        Collectors.mapping(ProblemGeneratorPermission::getProblemId, Collectors.toList())
                ));

        List<AuthManageProblemGeneratorMemberRes> members = users.stream()
                .filter(User::hasUserId)
                .filter(user -> user.getResolvedRole() == UserRole.PROBLEM_GENERATOR)
                .map(user -> new AuthManageProblemGeneratorMemberRes(
                        user.getUserId(),
                        permissionMap.getOrDefault(user.getUserId(), List.of())
                ))
                .toList();

        return new AuthManageProblemGeneratorGroupRes(members.size(), members);
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

    private List<String> normalizeProblemIds(List<String> problemIds) {
        return Optional.ofNullable(problemIds)
                .orElse(List.of())
                .stream()
                .map(problemId -> Optional.ofNullable(problemId)
                        .map(String::trim)
                        .orElse(""))
                .filter(problemId -> !problemId.isEmpty())
                .distinct()
                .toList();
    }

    private void validateProblemIds(List<String> problemIds) {
        if (problemIds.isEmpty()) {
            return;
        }

        Set<String> existingProblemIds = StreamSupport.stream(problemRepository.findAllById(problemIds).spliterator(), false)
                .map(Problem::getProblemId)
                .collect(Collectors.toSet());

        if (problemIds.stream().anyMatch(problemId -> !existingProblemIds.contains(problemId))) {
            throw new BusinessException(INVALID_PROBLEM_ID_MESSAGE, HttpStatus.BAD_REQUEST);
        }
    }

}
