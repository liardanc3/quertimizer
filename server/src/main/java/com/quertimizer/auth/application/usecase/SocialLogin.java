package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.SocialLoginInput;
import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;

import static com.quertimizer.auth.domain.model.AuthFailReason.OAUTH2_AUTHENTICATION_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class SocialLogin {

    private final AuthService authService;

    public Authentication execute(SocialLoginInput input) {
        // OAuth2 인증정보에서 provider와 attribute를 해석
        OAuth2AuthenticationToken authentication = resolveOAuth2Authentication(input.getAuthentication());

        // 소셜 로그인 인증결과를 생성
        return authService.loginWithOAuth2(
                authentication.getAuthorizedClientRegistrationId(),
                authentication.getPrincipal().getAttributes(),
                input.getAccessIp()
        );
    }

    private OAuth2AuthenticationToken resolveOAuth2Authentication(Authentication authentication) {
        // 소셜 로그인 성공 endpoint는 OAuth2AuthenticationToken만 허용
        if (authentication instanceof OAuth2AuthenticationToken oauth2Authentication) {
            return oauth2Authentication;
        }

        throw new BusinessException(OAUTH2_AUTHENTICATION_NOT_FOUND.getMessage(), HttpStatus.UNAUTHORIZED);
    }
}
