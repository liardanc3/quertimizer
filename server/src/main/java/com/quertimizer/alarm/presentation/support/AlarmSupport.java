package com.quertimizer.alarm.presentation.support;

import com.quertimizer.auth.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlarmSupport {

    private final AuthService authService;

    /**
     * Spring Security 인증 정보에서 현재 사용자 handle을 확인한다.
     *
     * @param authentication 현재 요청의 인증 정보
     */
    public String resolveCurrentHandle(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authService.resolveCurrentHandle(authentication.getName());
    }
}
