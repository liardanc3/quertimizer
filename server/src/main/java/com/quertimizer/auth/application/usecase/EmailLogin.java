package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.EmailLoginInput;
import com.quertimizer.auth.application.service.LoginService;
import com.quertimizer.auth.domain.policy.LoginPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailLogin {

    private final LoginService loginService;
    private final LoginPolicy loginPolicy;

    /**
     * 이메일 로그인 인증 결과를 생성하고 계정 상태와 접속 정보를 반영한다.
     *
     * <ol>
     *   <li>이메일 인증 결과 생성
     *   <li>차단 계정 검증
     *   <li>마지막 접속 정보 갱신 후 인증 결과 반환
     * </ol>
     *
     * @param input 이메일 로그인 입력
     */
    public Authentication execute(EmailLoginInput input) {
        Authentication authentication = loginService.getAuthentication(input.getEmail(), input.getPassword());

        loginPolicy.validateBlockedUser(authentication.getName());

        loginService.updateLastAccess(authentication.getName(), input.getAccessIp());
        return authentication;
    }
}
