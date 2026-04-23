package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CheckDuplicateEmail {

    private final AuthService authService;

    public boolean execute(String email) {
        return authService.isDuplicatedEmail(email);
    }

}
