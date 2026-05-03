package com.quertimizer.auth.domain.policy;

import com.quertimizer.global.exception.DomainRuleViolationException;
import com.quertimizer.global.exception.DomainRuleViolationType;
import com.quertimizer.user.domain.entity.User;

import static com.quertimizer.auth.domain.model.LoginFailReason.BLOCKED_USER;

public class LoginPolicy {

    public void validateBlockedUser(User user) {
        // Handle 설정 사용자 차단 상태 검증
        if (user != null && user.hasHandle() && user.isBlocked()) {
            throw new DomainRuleViolationException(BLOCKED_USER.getMessage(), DomainRuleViolationType.ACCESS_DENIED);
        }
    }
}
