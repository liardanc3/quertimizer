package com.quertimizer.auth.application.port.out;

public interface PasswordEncodingPort {

    String encode(String rawPassword);
}
