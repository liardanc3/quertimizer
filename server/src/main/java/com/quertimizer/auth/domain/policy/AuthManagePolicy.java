package com.quertimizer.auth.domain.policy;

import com.quertimizer.global.constant.UserRole;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.user.application.port.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import static com.quertimizer.auth.domain.model.AuthManageFailReason.LAST_ADMIN_PROTECTION;
import static com.quertimizer.auth.domain.model.AuthManageFailReason.PROBLEM_GENERATOR_REQUIRED;
import static com.quertimizer.auth.domain.model.AuthManageFailReason.SELF_ADMIN_REMOVAL_DENIED;
import static com.quertimizer.auth.domain.model.AuthManageFailReason.SENSITIVE_CONFIRMATION_REQUIRED;

@Component
@RequiredArgsConstructor
public class AuthManagePolicy {

    private final UserRepository userRepository;

    public void validateSensitiveConfirmation(String confirmationText) {
        if (!"ROLE_CHANGE_CONFIRMED".equals(confirmationText)) {
            throw new BusinessException(SENSITIVE_CONFIRMATION_REQUIRED.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * 마지막 Admin 역할 해제를 차단한다.
     *
     * <ol>
     *   <li>Admin 역할 해제 여부 확인
     *   <li>현재 Admin 수 검증
     * </ol>
     *
     * @param currentRole 변경 대상 사용자의 현재 역할
     * @param nextRole 변경하려는 다음 역할
     */
    public void validateAdminRoleChange(UserRole currentRole, UserRole nextRole) {
        if (currentRole != UserRole.ADMIN || nextRole == UserRole.ADMIN) {
            return;
        }

        long adminCount = userRepository.findAllByOrderByHandleAsc().stream()
                .filter(user -> user.getResolvedRole() == UserRole.ADMIN)
                .count();
        if (adminCount <= 1) {
            throw new BusinessException(LAST_ADMIN_PROTECTION.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    public void validateSelfAdminRemoval(String actorEmail, String targetEmail, UserRole currentRole, UserRole nextRole) {
        if (currentRole == UserRole.ADMIN
                && nextRole != UserRole.ADMIN
                && actorEmail != null
                && actorEmail.equalsIgnoreCase(targetEmail)) {
            throw new BusinessException(SELF_ADMIN_REMOVAL_DENIED.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * ProblemGenerator 역할 사용자만 문제 권한을 수정하게 한다.
     *
     * @param role 권한 수정 대상 사용자의 현재 역할
     */
    public void validateProblemGeneratorRole(UserRole role) {
        if (role != UserRole.PROBLEM_GENERATOR) {
            throw new BusinessException(PROBLEM_GENERATOR_REQUIRED.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}
