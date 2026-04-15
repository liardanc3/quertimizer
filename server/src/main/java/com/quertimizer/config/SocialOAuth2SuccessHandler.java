package com.quertimizer.config;

import com.quertimizer.service.UserAccountService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class SocialOAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserAccountService userAccountService;
    private final SecurityContextRepository securityContextRepository;

    @Value("${app.frontend-base-url}")
    private String frontendBaseUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2AuthenticationToken oauth2Authentication = resolveOAuth2Authentication(authentication);
        Authentication sessionAuthentication = userAccountService.loginWithOAuth2(
                oauth2Authentication.getAuthorizedClientRegistrationId(),
                oauth2Authentication.getPrincipal().getAttributes()
        );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(sessionAuthentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, request, response);
        clearAuthenticationAttributes(request);

        getRedirectStrategy().sendRedirect(
                request,
                response,
                buildFrontendSuccessUrl(oauth2Authentication.getAuthorizedClientRegistrationId())
        );
    }

    private OAuth2AuthenticationToken resolveOAuth2Authentication(Authentication authentication) {
        if (authentication instanceof OAuth2AuthenticationToken oauth2Authentication) {
            return oauth2Authentication;
        }

        throw new IllegalStateException("OAuth2 authentication is required.");
    }

    private String buildFrontendSuccessUrl(String provider) {
        return UriComponentsBuilder.fromUriString(frontendBaseUrl)
                .replaceQueryParam("socialLoginSuccess", provider)
                .build()
                .toUriString();
    }
}
