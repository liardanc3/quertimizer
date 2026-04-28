package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.SetupHandleInput;
import com.quertimizer.auth.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SetupHandle {

    private final AuthService authService;

    /**
     * 가입 직후 필요한 Handle을 설정한다.
     *
     * @param input 인증 이메일과 설정할 Handle 입력
     */
    public void execute(SetupHandleInput input) {
        authService.configureHandle(input.getAuthenticatedEmail(), input);
    }
}
