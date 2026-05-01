package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.SocialLoginInput;
import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.auth.application.service.LoginService;
import com.quertimizer.auth.domain.policy.LoginPolicy;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.user.domain.entity.User;
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
    private final LoginService loginService;
    private final LoginPolicy loginPolicy;

    /**
     * OAuth2 인증 결과로 소셜 로그인 인증 결과를 생성한다.
     *
     * <ol>
     *   <li>OAuth2 인증 정보 해석
     *   <li>OAuth2 사용자 조회 또는 생성
     *   <li>차단 계정 검증과 접속 정보 갱신
     *   <li>소셜 로그인 인증 결과 생성
     * </ol>
     *
     * @param input 소셜 로그인 입력
     */
    public Authentication execute(SocialLoginInput input) {
        OAuth2AuthenticationToken authentication = resolveOAuth2Authentication(input.getAuthentication());
        User user = authService.findOrCreateOAuth2User(
                authentication.getAuthorizedClientRegistrationId(), authentication.getPrincipal().getAttributes()
        );
        loginPolicy.validateBlockedUser(user.getEmail());
        loginService.updateLastAccess(user.getEmail(), input.getAccessIp());
        return loginService.getAuthentication(user);
    }

    private OAuth2AuthenticationToken resolveOAuth2Authentication(Authentication authentication) {
        // 소셜 로그인 성공 endpoint는 OAuth2AuthenticationToken만 허용
        if (authentication instanceof OAuth2AuthenticationToken oauth2Authentication) {
            return oauth2Authentication;
        }

        throw new BusinessException(OAUTH2_AUTHENTICATION_NOT_FOUND.getMessage(), HttpStatus.UNAUTHORIZED);
    }
}
