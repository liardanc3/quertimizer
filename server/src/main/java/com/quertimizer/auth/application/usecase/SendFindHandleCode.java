package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.AccountRecoveryEmailInput;
import com.quertimizer.auth.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SendFindHandleCode {

    private final AuthService authService;

    public void execute(AccountRecoveryEmailInput input) {
        authService.sendFindHandleCode(input);
    }

}
