package com.quertimizer.auth.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AccountRecoveryEmailInput {

    private final String email;
}
