package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.ResetPasswordInput;
import com.quertimizer.auth.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResetPassword {

    private final AuthService authService;

    /**
     * 인증이 끝난 이메일의 비밀번호를 재설정한다.
     *
     * @param input 비밀번호 재설정 입력
     */
    public void execute(ResetPasswordInput input) {
        authService.resetPassword(input);
    }
}
