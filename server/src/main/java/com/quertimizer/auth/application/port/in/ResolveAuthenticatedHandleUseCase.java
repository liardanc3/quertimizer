package com.quertimizer.auth.application.port.in;

public interface ResolveAuthenticatedHandleUseCase {

    String execute(String authenticatedEmail);
}
