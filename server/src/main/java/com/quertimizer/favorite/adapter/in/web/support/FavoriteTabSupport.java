package com.quertimizer.favorite.adapter.in.web.support;

import com.quertimizer.auth.application.port.in.ResolveAuthenticatedEmailUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FavoriteTabSupport {

    private final ResolveAuthenticatedEmailUseCase resolveAuthenticatedEmail;

    public String resolveCurrentUserEmail(Authentication authentication) {
        // Spring Security 인증 정보 기준 현재 사용자 이메일 확인
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return resolveAuthenticatedEmail.execute(authentication.getName());
    }
}
