package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.VerifyCodeInput;
import com.quertimizer.auth.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VerifyFindPasswordCode {

    private final AuthService authService;

    /**
     * 비밀번호 찾기 인증코드를 검증한다.
     *
     * @param input 인증코드 검증 입력
     */
    public void execute(VerifyCodeInput input) {
        authService.verifyFindPasswordCode(input);
    }
}
