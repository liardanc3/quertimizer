package com.quertimizer.auth.domain.policy;

import com.quertimizer.user.domain.model.UserRole;
import com.quertimizer.global.exception.DomainRuleViolationException;
import com.quertimizer.global.exception.DomainRuleViolationType;
import org.springframework.stereotype.Component;

import static com.quertimizer.auth.domain.model.AuthManageFailReason.LAST_ADMIN_PROTECTION;
import static com.quertimizer.auth.domain.model.AuthManageFailReason.SELF_ADMIN_REMOVAL_DENIED;
import static com.quertimizer.auth.domain.model.AuthManageFailReason.SENSITIVE_CONFIRMATION_REQUIRED;

@Component
public class AuthManagePolicy {

    public void validateSensitiveConfirmation(String confirmationText) {
        // 민감 작업 확인 값 검증
        if (!"ROLE_CHANGE_CONFIRMED".equals(confirmationText)) {
            throw new DomainRuleViolationException(SENSITIVE_CONFIRMATION_REQUIRED.getMessage(), DomainRuleViolationType.INVALID_REQUEST);
        }
    }

    public void validateAdminRoleChange(UserRole currentRole, UserRole nextRole, long adminCount) {
        // 마지막 Admin 권한 제거 여부 검증
        if (currentRole != UserRole.ADMIN || nextRole == UserRole.ADMIN) {
            return;
        }

        if (adminCount <= 1) {
            throw new DomainRuleViolationException(LAST_ADMIN_PROTECTION.getMessage(), DomainRuleViolationType.INVALID_REQUEST);
        }
    }

    public void validateSelfAdminRemoval(String actorEmail, String targetEmail, UserRole currentRole, UserRole nextRole) {
        // 자기 자신의 Admin 권한 제거 여부 검증
        if (currentRole == UserRole.ADMIN
                && nextRole != UserRole.ADMIN
                && actorEmail != null
                && actorEmail.equalsIgnoreCase(targetEmail)) {
            throw new DomainRuleViolationException(SELF_ADMIN_REMOVAL_DENIED.getMessage(), DomainRuleViolationType.INVALID_REQUEST);
        }
    }
}
