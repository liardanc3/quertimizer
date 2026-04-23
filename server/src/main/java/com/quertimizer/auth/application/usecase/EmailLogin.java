package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.EmailLoginInput;
import com.quertimizer.auth.application.service.LoginService;
import com.quertimizer.auth.domain.policy.LoginPolicy;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.global.util.CanonicalCode;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.quertimizer.auth.domain.model.LoginFailReason.INVALID_EMAIL_OR_PASSWORD;
import static org.springframework.security.authentication.UsernamePasswordAuthenticationToken.*;

@Component
@RequiredArgsConstructor
public class EmailLogin {

    private final AuthenticationManager authenticationManager;
    private final LoginService loginService;
    private final LoginPolicy loginPolicy;

    @CanonicalCode
    public Authentication execute(EmailLoginInput input) {

        // 이메일 + 패스워드 기반 인증정보 생성
        Authentication authentication =
                Optional.of(authenticationManager.authenticate(unauthenticated(input.getEmail(), input.getPassword())))
                        .orElseThrow(() -> new BusinessException(INVALID_EMAIL_OR_PASSWORD.getMessage(), HttpStatus.UNAUTHORIZED));

        // 차단 계정 여부 확인
        loginPolicy.validateBlockedUser(authentication.getName());

        // 마지막 접속 정보 갱신
        loginService.updateLastAccess(authentication.getName(), input.getAccessIp());

        // 인증정보 반환
        return authentication;
    }
}
