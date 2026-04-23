package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.AccountRecoveryCodeInput;
import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.auth.application.result.FoundHandleResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FindHandle {

    private final AuthService authService;

    public FoundHandleResult execute(AccountRecoveryCodeInput input) {
        return authService.findHandle(input);
    }

}
