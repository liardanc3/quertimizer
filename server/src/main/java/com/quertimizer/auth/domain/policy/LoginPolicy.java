package com.quertimizer.auth.domain.policy;

import com.quertimizer.global.exception.DomainRuleViolationException;
import com.quertimizer.global.exception.DomainRuleViolationType;
import org.springframework.stereotype.Component;

import static com.quertimizer.auth.domain.model.LoginFailReason.BLOCKED_USER;

@Component
public class LoginPolicy {

    public void validateBlockedUser(boolean hasHandle, boolean blocked) {
        // Handle 설정 사용자 차단 상태 검증
        if (hasHandle && blocked) {
            throw new DomainRuleViolationException(BLOCKED_USER.getMessage(), DomainRuleViolationType.ACCESS_DENIED);
        }
    }
}
