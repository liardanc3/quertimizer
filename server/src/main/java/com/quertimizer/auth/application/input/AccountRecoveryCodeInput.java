package com.quertimizer.auth.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AccountRecoveryCodeInput {

    private final String email;
    private final String code;
}
