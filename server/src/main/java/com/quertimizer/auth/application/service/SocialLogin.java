package com.quertimizer.auth.application.service;

import com.quertimizer.auth.application.port.in.SocialLoginUseCase;
import com.quertimizer.auth.application.input.SocialLoginInput;
import com.quertimizer.auth.application.output.AuthenticatedUserOutput;
import com.quertimizer.auth.domain.policy.LoginPolicy;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SocialLogin implements SocialLoginUseCase {

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
    @Override
    public AuthenticatedUserOutput execute(SocialLoginInput input) {
        User user = authService.findOrCreateOAuth2User(input.getProvider(), input.getAttributes());
        loginPolicy.validateBlockedUser(user);
        loginService.updateLastAccess(user.getEmail(), input.getAccessIp());
        return AuthenticatedUserOutput.from(user);
    }
}
