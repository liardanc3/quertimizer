package com.quertimizer.auth.domain.policy;

import com.quertimizer.global.exception.DomainRuleViolationException;
import com.quertimizer.global.exception.DomainRuleViolationType;

import java.util.Locale;

import static com.quertimizer.auth.domain.model.SignupFailReason.DUPLICATED_EMAIL;
import static com.quertimizer.auth.domain.model.SignupFailReason.DUPLICATED_HANDLE;

public class SignupPolicy {

    public void validateAvailableHandle(String handle, boolean handleExists) {
        // Handle 입력값과 중복 여부 검증
        if (handle == null || handle.trim().isEmpty() || handleExists) {
            throw new DomainRuleViolationException(DUPLICATED_HANDLE.getMessage(), DomainRuleViolationType.DUPLICATED_RESOURCE);
        }
    }

    public void validateAvailableEmail(String email, boolean emailExists) {
        // 이메일 입력값과 중복 여부 검증
        String normalizedEmail = email != null ? email.trim().toLowerCase(Locale.ROOT) : "";
        if (normalizedEmail.isEmpty() || emailExists) {
            throw new DomainRuleViolationException(DUPLICATED_EMAIL.getMessage(), DomainRuleViolationType.DUPLICATED_RESOURCE);
        }
    }
}
