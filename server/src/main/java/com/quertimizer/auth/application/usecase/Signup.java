package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.input.SignupInput;
import com.quertimizer.auth.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class Signup {

    private final AuthService authService;

    public Authentication execute(SignupInput input) {
        return authService.signup(input);
    }

}
