package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.ResetPasswordInput;
import com.quertimizer.auth.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResetPassword {

    private final AuthService authService;

    public void execute(ResetPasswordInput input) {
        // 비밀번호를 재설정
        authService.resetPassword(input);
    }
}
