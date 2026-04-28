package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.VerifyCodeInput;
import com.quertimizer.auth.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VerifySignupCode {

    private final AuthService authService;

    /**
     * 회원가입용 인증코드를 검증한다.
     *
     * @param input 인증코드 검증 입력
     */
    public void execute(VerifyCodeInput input) {
        authService.verifySignupCode(input);
    }
}
