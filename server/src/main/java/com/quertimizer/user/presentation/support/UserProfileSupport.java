package com.quertimizer.user.presentation.support;

import com.quertimizer.auth.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserProfileSupport {

    private final AuthService authService;

    public String resolveCurrentHandle(Authentication authentication) {
        // 현재 인증 기준 Handle을 해석
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authService.resolveCurrentHandle(authentication.getName());
    }
}
