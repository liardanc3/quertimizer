package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class SocialLogin {

    private final AuthService authService;

    public Authentication execute(String provider, Map<String, Object> attributes, HttpServletRequest httpRequest) {
        return authService.loginWithOAuth2(provider, attributes, httpRequest);
    }

}
