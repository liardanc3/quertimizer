package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.SetupHandleInput;
import com.quertimizer.auth.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SetupHandle {

    private final AuthService authService;

    public void execute(SetupHandleInput input) {
        // 가입 직후 Handle을 설정
        authService.configureHandle(input.getAuthenticatedEmail(), input);
    }
}
