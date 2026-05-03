package com.quertimizer.auth.application.port.out;

public interface AuthenticationPort {

    String authenticateByEmailPassword(String email, String password);
}
