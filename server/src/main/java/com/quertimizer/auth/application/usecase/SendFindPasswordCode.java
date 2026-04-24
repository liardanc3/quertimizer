package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.SendCodeInput;
import com.quertimizer.auth.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendFindPasswordCode {

    private final AuthService authService;

    public void execute(SendCodeInput input) {
        // 비밀번호 찾기 인증코드를 전송
        authService.sendFindPasswordCode(input);
    }
}
