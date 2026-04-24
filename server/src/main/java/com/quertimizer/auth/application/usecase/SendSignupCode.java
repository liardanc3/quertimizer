package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.SendCodeInput;
import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.global.util.CanonicalCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@CanonicalCode
@Component
@RequiredArgsConstructor
public class SendSignupCode {

    private final AuthService authService;

    public void execute(SendCodeInput input) {
        // 인증코드 전송
        authService.sendSignupCode(input.getEmail());
    }
}
