package com.quertimizer.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class SocialOAuth2FailureHandler implements AuthenticationFailureHandler {

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        response.sendRedirect(UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .replaceQueryParam("socialLoginError", resolveProvider(request))
                .build()
                .toUriString());
    }

    private String resolveProvider(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (requestUri == null || requestUri.isBlank()) {
            return "oauth2";
        }

        int lastSlashIndex = requestUri.lastIndexOf('/');
        if (lastSlashIndex < 0 || lastSlashIndex == requestUri.length() - 1) {
            return "oauth2";
        }

        return requestUri.substring(lastSlashIndex + 1);
    }
}
