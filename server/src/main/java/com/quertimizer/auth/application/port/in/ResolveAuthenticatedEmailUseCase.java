package com.quertimizer.auth.application.port.in;

public interface ResolveAuthenticatedEmailUseCase {

    String execute(String authenticatedEmail);
}
