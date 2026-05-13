package com.quertimizer.auth.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.auth.application.port.in.ResolveAuthenticatedHandleUseCase;
import com.quertimizer.auth.domain.model.AuthUser;
import com.quertimizer.auth.application.port.out.AuthUserPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ResolveAuthenticatedHandle implements ResolveAuthenticatedHandleUseCase {

    private final AuthUserPort userRepository;

    /**
     * 인증 이메일 기준 현재 사용자 handle을 조회한다.
     *
     * @param authenticatedEmail 현재 요청의 인증 이메일
     */
    @Override
    @Log("인증 Handle 확인")
    public String execute(String authenticatedEmail) {
        return userRepository.findByEmailIgnoreCase(normalizeEmail(authenticatedEmail))
                .map(AuthUser::getHandle)
                .filter(handle -> !handle.isBlank())
                .orElse(null);
    }

    private String normalizeEmail(String email) {
        // 인증 이메일 공백 제거와 소문자 변환
        return Optional.ofNullable(email)
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse("");
    }
}
