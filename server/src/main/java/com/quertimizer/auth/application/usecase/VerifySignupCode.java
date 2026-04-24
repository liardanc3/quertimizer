package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.VerifyCodeInput;
import com.quertimizer.auth.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VerifySignupCode {

    private final AuthService authService;

    public void execute(VerifyCodeInput input) {
        // 회원가입 인증코드를 확인
        authService.verifySignupCode(input);
    }
}
