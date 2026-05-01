package com.quertimizer.dashboard.presentation.support;

import com.quertimizer.auth.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DashboardSupport {

    private final AuthService authService;

    public String resolveCurrentHandle(Authentication authentication) {
        // Spring Security 인증 정보 기준 현재 사용자 handle 확인
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authService.resolveCurrentHandle(authentication.getName());
    }
}
