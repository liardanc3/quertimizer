package com.quertimizer.auth.domain.policy;

import com.quertimizer.global.exception.DomainRuleViolationException;
import com.quertimizer.global.exception.DomainRuleViolationType;

import static com.quertimizer.auth.domain.model.AuthRateLimitConstant.CODE_HOUR_LIMIT;
import static com.quertimizer.auth.domain.model.AuthRateLimitConstant.CODE_MINUTE_LIMIT;
import static com.quertimizer.auth.domain.model.AuthRateLimitConstant.CODE_VERIFY_FAILURE_LIMIT;
import static com.quertimizer.auth.domain.model.AuthRateLimitConstant.LOGIN_FAILURE_LIMIT;
import static com.quertimizer.auth.domain.model.AuthRateLimitConstant.PASSWORD_RESET_LIMIT;

public class AuthRateLimitPolicy {

    public void validateLoginAllowed(long failureCount) {
        // 로그인 실패 횟수 제한 검증
        if (failureCount >= LOGIN_FAILURE_LIMIT) {
            throw tooManyRequests("로그인 실패 횟수가 많습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    public void validateCodeIssueAllowed(long minuteCount, long hourCount) {
        // 인증코드 발급 횟수 제한 검증
        if (minuteCount >= CODE_MINUTE_LIMIT || hourCount >= CODE_HOUR_LIMIT) {
            throw tooManyRequests("인증코드 요청이 많습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    public boolean isCodeVerificationFailureLimited(long failureCount) {
        // 인증코드 검증 실패 횟수 제한 여부 반환
        return failureCount >= CODE_VERIFY_FAILURE_LIMIT;
    }

    public void validatePasswordResetAllowed(long resetCount) {
        // 비밀번호 재설정 요청 횟수 제한 검증
        if (resetCount >= PASSWORD_RESET_LIMIT) {
            throw tooManyRequests("비밀번호 재설정 요청이 많습니다. 잠시 후 다시 시도해 주세요.");
        }
    }

    private DomainRuleViolationException tooManyRequests(String message) {
        // 요청 제한 도메인 예외 생성
        return new DomainRuleViolationException(message, DomainRuleViolationType.REQUEST_LIMIT_EXCEEDED);
    }
}
