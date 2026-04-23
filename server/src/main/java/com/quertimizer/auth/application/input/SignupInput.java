package com.quertimizer.auth.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class SignupInput {

    private final String password;
    private final String email;
}
