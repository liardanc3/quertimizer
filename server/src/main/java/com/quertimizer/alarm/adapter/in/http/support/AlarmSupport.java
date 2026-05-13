package com.quertimizer.alarm.adapter.in.http.support;

import com.quertimizer.auth.application.port.in.ResolveAuthenticatedHandleUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlarmSupport {

    private final ResolveAuthenticatedHandleUseCase resolveAuthenticatedHandle;

    public boolean isAuthenticated(Authentication authentication) {
        // Spring Security 인증 정보 유효 여부 확인
        return authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName());
    }

    public String resolveCurrentHandle(Authentication authentication) {
        // Spring Security 인증 정보 기준 현재 사용자 handle 확인
        if (!isAuthenticated(authentication)) {
            return null;
        }

        return resolveAuthenticatedHandle.execute(authentication.getName());
    }
}
