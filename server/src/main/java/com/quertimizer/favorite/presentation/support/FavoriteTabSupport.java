package com.quertimizer.favorite.presentation.support;

import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FavoriteTabSupport {

    private final AuthService authService;

    public String resolveCurrentUserEmail(Authentication authentication) {
        // Spring Security 인증 정보 기준 현재 사용자 이메일 확인
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authService.findAuthenticatedUser(authentication.getName())
                .map(User::getEmail)
                .orElse(null);
    }
}
