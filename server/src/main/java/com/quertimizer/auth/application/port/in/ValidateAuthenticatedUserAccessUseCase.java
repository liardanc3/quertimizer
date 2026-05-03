package com.quertimizer.auth.application.port.in;

public interface ValidateAuthenticatedUserAccessUseCase {

    void execute(String authenticatedEmail);
}
