package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.input.UpdateProblemGeneratorPermissionsInput;
import com.quertimizer.auth.application.input.UpdateUserRoleInput;
import com.quertimizer.auth.application.output.AuthManageOutput;
import com.quertimizer.auth.application.output.AuthManageUserRowOutput;
import com.quertimizer.auth.domain.policy.AuthManagePolicy;
import com.quertimizer.global.constant.UserRole;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.problem.application.port.ProblemGeneratorPermissionRepository;
import com.quertimizer.problem.application.port.ProblemRepository;
import com.quertimizer.problem.application.port.ProblemSetRepository;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.domain.entity.ProblemGeneratorPermission;
import com.quertimizer.problem.domain.entity.ProblemSet;
import com.quertimizer.problem.domain.policy.ProblemManagementPolicy;
import com.quertimizer.user.application.port.UserRepository;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

import static com.quertimizer.auth.domain.model.AuthManageFailReason.INVALID_PERMISSION_KEY;
import static com.quertimizer.auth.domain.model.AuthManageFailReason.INVALID_ROLE;
import static com.quertimizer.auth.domain.model.AuthManageFailReason.USER_NOT_FOUND;
import static com.quertimizer.problem.domain.model.ProblemPermissionKey.NEW;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AuthManageService {

    private final UserRepository userRepository;
    private final ProblemRepository problemRepository;
    private final ProblemSetRepository problemSetRepository;
    private final ProblemGeneratorPermissionRepository problemGeneratorPermissionRepository;
    private final ProblemManagementPolicy problemManagementPolicy;
    private final AuthManagePolicy authManagePolicy;

    public AuthManageOutput getAuthManage() {
        // 권한 설정 화면에 필요한 사용자와 권한 목록을 조회
        List<User> users = userRepository.findAllByOrderByHandleAsc();
        Map<String, List<String>> permissionMap = problemGeneratorPermissionRepository.findAllByOrderByIdHandleAscIdProblemIdAsc().stream()
                .collect(Collectors.groupingBy(
                        ProblemGeneratorPermission::getHandle,
                        Collectors.mapping(permission -> normalizeStoredPermissionKey(permission.getProblemId()), Collectors.toList())
                ));

        List<AuthManageUserRowOutput> members = users.stream()
                .filter(User::hasHandle)
                .map(user -> new AuthManageUserRowOutput(
                        user.getHandle(),
                        resolveRoleValue(user.getResolvedRole()),
                        user.getResolvedRole() == UserRole.PROBLEM_GENERATOR
                                ? sortPermissionKeys(permissionMap.getOrDefault(user.getHandle(), List.of()))
                                : List.of()
                ))
                .toList();

        return new AuthManageOutput(members);
    }

    @Transactional
    public void updateUserRole(UpdateUserRoleInput input) {
        // 변경 대상 사용자와 다음 역할을 확정
        User user = findUser(input.getHandle());
        UserRole nextRole = normalizeRole(input.getRole());
        UserRole currentRole = user.getResolvedRole();

        authManagePolicy.validateSensitiveConfirmation(input.getConfirmationText());
        authManagePolicy.validateSelfAdminRemoval(input.getActorEmail(), user.getEmail(), currentRole, nextRole);
        authManagePolicy.validateAdminRoleChange(currentRole, nextRole);

        // 사용자 역할을 수정하고 불필요한 권한을 정리
        user.changeRole(nextRole);
        if (nextRole != UserRole.PROBLEM_GENERATOR) {
            problemGeneratorPermissionRepository.deleteAllByIdHandle(input.getHandle());
        }

        log.info("Auth role changed actor={} target={} before={} after={}",
                input.getActorEmail(), input.getHandle(), currentRole, nextRole);
    }

    @Transactional
    public void updateProblemGeneratorPermissions(UpdateProblemGeneratorPermissionsInput input) {
        // ProblemGenerator 사용자만 문제 권한을 수정
        User user = findUser(input.getHandle());
        authManagePolicy.validateProblemGeneratorRole(user.getResolvedRole());

        // 저장할 권한 키를 정규화하고 유효성을 검증
        List<String> normalizedPermissionKeys = normalizePermissionKeys(input.getPermissionKeys());
        validatePermissionKeys(normalizedPermissionKeys);

        // 기존 권한을 교체하고 새 권한을 저장
        problemGeneratorPermissionRepository.deleteAllByIdHandle(input.getHandle());
        if (!normalizedPermissionKeys.isEmpty()) {
            problemGeneratorPermissionRepository.saveAll(normalizedPermissionKeys.stream()
                    .map(permissionKey -> ProblemGeneratorPermission.create(input.getHandle(), permissionKey))
                    .toList());
        }
    }

    private User findUser(String handle) {
        // handle 기준으로 변경 대상 사용자를 찾는다
        return userRepository.findByHandle(handle)
                .orElseThrow(() -> new BusinessException(USER_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
    }

    private UserRole normalizeRole(String role) {
        // 요청 역할 문자열을 내부 역할 값으로 정규화
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

    private List<String> normalizePermissionKeys(List<String> permissionKeys) {
        // 중복과 공백을 제거한 권한 키 목록으로 정리
        return Optional.ofNullable(permissionKeys)
                .orElse(List.of())
                .stream()
                .map(this::normalizePermissionKey)
                .filter(permissionKey -> !permissionKey.isEmpty())
                .distinct()
                .toList();
    }

    private void validatePermissionKeys(List<String> permissionKeys) {
        // 저장할 권한이 없으면 추가 검증 없이 종료
        if (permissionKeys.isEmpty()) {
            return;
        }

        // 존재하는 문제와 테이블셋 권한만 허용
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
        // 권한 키를 problem domain 정책 기준으로 정규화
        return problemManagementPolicy.normalizePermissionKey(permissionKey);
    }

    private String normalizeStoredPermissionKey(String permissionKey) {
        // 저장된 권한 키도 화면 표시에 맞는 형식으로 정리
        String normalizedPermissionKey = normalizePermissionKey(permissionKey);
        return normalizedPermissionKey.isEmpty()
                ? Optional.ofNullable(permissionKey).map(String::trim).orElse("")
                : normalizedPermissionKey;
    }

    private boolean isScopedProblemId(String permissionKey) {
        // 문제 번호 형식 권한인지 확인
        return problemManagementPolicy.isScopedProblemId(permissionKey);
    }

    private boolean isScopedProblemSetId(String permissionKey) {
        // 테이블셋 번호 형식 권한인지 확인
        return problemManagementPolicy.isScopedProblemSetId(permissionKey);
    }

    private List<String> sortPermissionKeys(List<String> permissionKeys) {
        // NEW, 테이블셋, 문제 순으로 권한 키를 정렬
        return permissionKeys.stream()
                .filter(permissionKey -> !permissionKey.isBlank())
                .distinct()
                .sorted(Comparator.comparingInt(this::resolvePermissionOrder).thenComparing(String::compareTo))
                .toList();
    }

    private int resolvePermissionOrder(String permissionKey) {
        // 권한 키 유형별 정렬 우선순위를 계산
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
        // 화면에서 사용하는 역할 문자열로 변환
        return switch (role) {
            case ADMIN -> "admin";
            case PROBLEM_GENERATOR -> "problemGenerator";
            default -> "user";
        };
    }

}
