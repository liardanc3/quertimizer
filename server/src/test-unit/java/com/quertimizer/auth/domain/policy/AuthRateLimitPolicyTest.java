package com.quertimizer.auth.domain.policy;

import com.quertimizer.global.exception.DomainRuleViolationException;
import com.quertimizer.global.exception.DomainRuleViolationType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.quertimizer.auth.domain.model.AuthRateLimitConstant.CODE_MINUTE_LIMIT;
import static com.quertimizer.auth.domain.model.AuthRateLimitConstant.LOGIN_FAILURE_LIMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AuthRateLimitPolicy")
class AuthRateLimitPolicyTest {

    private final AuthRateLimitPolicy authRateLimitPolicy = new AuthRateLimitPolicy();

    @Nested
    @DisplayName("validateLoginAllowed")
    class ValidateLoginAllowed {

        @Test
        @DisplayName("성공 (제한 미만)")
        void successWhenFailureCountBelowLimit() {
            // given
            long failureCount = LOGIN_FAILURE_LIMIT - 1;

            // when
            authRateLimitPolicy.validateLoginAllowed(failureCount);

            // then
        }

        @Test
        @DisplayName("실패 (제한 이상)")
        void failWhenFailureCountOverLimit() {
            // given
            long failureCount = LOGIN_FAILURE_LIMIT;

            // when & then
            assertThatThrownBy(() -> authRateLimitPolicy.validateLoginAllowed(failureCount))
                    .isInstanceOf(DomainRuleViolationException.class)
                    .extracting("type")
                    .isEqualTo(DomainRuleViolationType.REQUEST_LIMIT_EXCEEDED);
        }
    }

    @Nested
    @DisplayName("validateCodeIssueAllowed")
    class ValidateCodeIssueAllowed {

        @Test
        @DisplayName("실패 (분당 제한 이상)")
        void failWhenMinuteCountOverLimit() {
            // given
            long minuteCount = CODE_MINUTE_LIMIT;

            // when & then
            assertThatThrownBy(() -> authRateLimitPolicy.validateCodeIssueAllowed(minuteCount, 0))
                    .isInstanceOf(DomainRuleViolationException.class);
        }
    }

    @Nested
    @DisplayName("isCodeVerificationFailureLimited")
    class IsCodeVerificationFailureLimited {

        @Test
        @DisplayName("성공 (제한 여부 반환)")
        void successWhenFailureCountChecked() {
            // given

            // when
            boolean limited = authRateLimitPolicy.isCodeVerificationFailureLimited(Long.MAX_VALUE);

            // then
            assertThat(limited).isTrue();
        }
    }
}
