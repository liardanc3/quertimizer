package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.SendCodeInput;
import com.quertimizer.auth.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendSignupCode {

    private final AuthService authService;

    /**
     * 회원가입용 인증코드를 전송한다.
     *
     * @param input 인증코드 전송 입력
     */
    public void execute(SendCodeInput input) {
        authService.sendSignupCode(input);
    }
}
