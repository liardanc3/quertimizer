package com.quertimizer.user.adapter.in.http.support;

import com.quertimizer.auth.application.port.in.ResolveAuthenticatedHandleUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProfileSupport {

    private final ResolveAuthenticatedHandleUseCase resolveAuthenticatedHandle;

    public String resolveCurrentHandle(Authentication authentication) {
        // Spring Security 인증 정보 기준 현재 사용자 handle 확인
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return resolveAuthenticatedHandle.execute(authentication.getName());
    }

    public String resolveCurrentEmail(Authentication authentication) {
        // Spring Security 인증 정보 기준 현재 사용자 email 확인
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authentication.getName();
    }
}
