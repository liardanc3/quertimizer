package com.quertimizer.auth.application.usecase;

import com.quertimizer.auth.application.result.AuthenticatedSessionResult;
import com.quertimizer.auth.application.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetAuthenticatedSession {

    private final AuthService authService;

    public AuthenticatedSessionResult execute(String authenticatedEmail) {
        return authService.findAuthenticatedUser(authenticatedEmail)
                .map(user -> AuthenticatedSessionResult.authenticated(
                        user.getHandle(),
                        user.getResolvedDefaultDbms(),
                        user.getResolvedRole(),
                        !user.hasHandle()
                ))
                .orElseGet(AuthenticatedSessionResult::unauthenticated);
    }

}
