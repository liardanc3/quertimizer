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
        // 현재 인증 기준 이메일을 해석
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authService.findAuthenticatedUser(authentication.getName())
                .map(User::getEmail)
                .orElse(null);
    }
}
