package com.quertimizer.auth.domain.policy;

import com.quertimizer.global.exception.DomainRuleViolationException;
import com.quertimizer.global.exception.DomainRuleViolationType;
import org.springframework.stereotype.Component;

import static com.quertimizer.auth.domain.model.AuthRateLimitConstant.CODE_HOUR_LIMIT;
import static com.quertimizer.auth.domain.model.AuthRateLimitConstant.CODE_MINUTE_LIMIT;
import static com.quertimizer.auth.domain.model.AuthRateLimitConstant.CODE_VERIFY_FAILURE_LIMIT;
import static com.quertimizer.auth.domain.model.AuthRateLimitConstant.LOGIN_FAILURE_LIMIT;
import static com.quertimizer.auth.domain.model.AuthRateLimitConstant.PASSWORD_RESET_LIMIT;
import static com.quertimizer.auth.domain.model.AuthFailReason.CODE_ISSUE_RATE_LIMIT_EXCEEDED;
import static com.quertimizer.auth.domain.model.AuthFailReason.LOGIN_RATE_LIMIT_EXCEEDED;
import static com.quertimizer.auth.domain.model.AuthFailReason.PASSWORD_RESET_RATE_LIMIT_EXCEEDED;

@Component
public class AuthRateLimitPolicy {

    public void validateLoginAllowed(long failureCount) {
        // 로그인 실패 횟수 제한 검증
        if (failureCount >= LOGIN_FAILURE_LIMIT) {
            throw new DomainRuleViolationException(LOGIN_RATE_LIMIT_EXCEEDED.getMessage(), DomainRuleViolationType.REQUEST_LIMIT_EXCEEDED);
        }
    }

    public void validateCodeIssueAllowed(long minuteCount, long hourCount) {
        // 인증코드 발급 횟수 제한 검증
        if (minuteCount >= CODE_MINUTE_LIMIT || hourCount >= CODE_HOUR_LIMIT) {
            throw new DomainRuleViolationException(CODE_ISSUE_RATE_LIMIT_EXCEEDED.getMessage(), DomainRuleViolationType.REQUEST_LIMIT_EXCEEDED);
        }
    }

    public boolean isCodeVerificationFailureLimited(long failureCount) {
        // 인증코드 검증 실패 횟수 제한 여부 반환
        return failureCount >= CODE_VERIFY_FAILURE_LIMIT;
    }

    public void validatePasswordResetAllowed(long resetCount) {
        // 비밀번호 재설정 요청 횟수 제한 검증
        if (resetCount >= PASSWORD_RESET_LIMIT) {
            throw new DomainRuleViolationException(PASSWORD_RESET_RATE_LIMIT_EXCEEDED.getMessage(), DomainRuleViolationType.REQUEST_LIMIT_EXCEEDED);
        }
    }
}
