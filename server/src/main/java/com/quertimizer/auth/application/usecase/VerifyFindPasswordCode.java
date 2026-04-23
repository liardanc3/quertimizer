package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.AccountRecoveryCodeInput;
import com.quertimizer.auth.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class VerifyFindPasswordCode {

    private final AuthService authService;

    public void execute(AccountRecoveryCodeInput input) {
        authService.verifyFindPasswordCode(input);
    }

}
