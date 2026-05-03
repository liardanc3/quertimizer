package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.port.in.ValidateAuthenticatedUserAccessUseCase;
import com.quertimizer.auth.domain.policy.LoginPolicy;
import com.quertimizer.user.application.port.out.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ValidateAuthenticatedUserAccess implements ValidateAuthenticatedUserAccessUseCase {

    private final UserRepositoryPort userRepository;
    private final LoginPolicy loginPolicy;

    /**
     * 인증 이메일 기준 현재 사용자 접근 가능 여부를 검증한다.
     *
     * @param authenticatedEmail 현재 요청의 인증 이메일
     */
    @Override
    public void execute(String authenticatedEmail) {
        userRepository.findByEmailIgnoreCase(normalizeEmail(authenticatedEmail))
                .ifPresent(loginPolicy::validateBlockedUser);
    }

    private String normalizeEmail(String email) {
        // 인증 이메일 공백 제거와 소문자 변환
        return Optional.ofNullable(email)
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse("");
    }
}
