package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.EmailLoginInput;
import com.quertimizer.auth.application.service.LoginService;
import com.quertimizer.auth.domain.policy.LoginPolicy;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.global.util.CanonicalCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import static com.quertimizer.auth.domain.model.LoginFailReason.INVALID_EMAIL_OR_PASSWORD;

@Component
@RequiredArgsConstructor
public class EmailLogin {

    private final AuthenticationManager authenticationManager;
    private final LoginService loginService;
    private final LoginPolicy loginPolicy;

    @CanonicalCode
    public Authentication execute(EmailLoginInput input) {

        // 이메일 + 패스워드 기반 인증
        Authentication authentication = authenticate(input);

        // 차단 계정 여부 확인
        loginPolicy.validateBlockedUser(authentication.getName());

        // 마지막 접속 정보 갱신
        loginService.updateLastAccess(authentication.getName(), input.getAccessIp());

        // 인증정보 반환
        return authentication;
    }

    // 이메일 + 패스워드 기반 인증 (Spring Security)
    private Authentication authenticate(EmailLoginInput input) {
        try {
            return authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(input.getEmail(), input.getPassword())
            );
        } catch (AuthenticationException exception) {
            throw new BusinessException(INVALID_EMAIL_OR_PASSWORD.getMessage(), HttpStatus.UNAUTHORIZED);
        }
    }

}
