package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.EmailLoginInput;
import com.quertimizer.auth.application.service.LoginService;
import com.quertimizer.auth.domain.policy.LoginPolicy;
import com.quertimizer.global.util.CanonicalCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@CanonicalCode
@Component
@RequiredArgsConstructor
public class EmailLogin {

    private final LoginService loginService;
    private final LoginPolicy loginPolicy;

    public Authentication execute(EmailLoginInput input) {
        // 이메일 + 패스워드 기반 인증결과 생성
        Authentication authentication = loginService.getAuthentication(input.getEmail(), input.getPassword());

        // 차단 계정 여부 확인
        loginPolicy.validateBlockedUser(authentication.getName());

        // 마지막 접속 정보 갱신
        loginService.updateLastAccess(authentication.getName(), input.getAccessIp());

        // 인증결과 반환
        return authentication;
    }
}
