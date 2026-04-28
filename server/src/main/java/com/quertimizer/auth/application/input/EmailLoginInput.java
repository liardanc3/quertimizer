package com.quertimizer.auth.application.input;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Locale;
import java.util.Optional;

@Data
@AllArgsConstructor
public class EmailLoginInput {

    private final String email;
    private final String password;
    private final String accessIp;

    public static EmailLoginInput of(String email, String password, String accessIp) {
        return new EmailLoginInput(normalizeEmail(email), password, normalizeAccessIp(accessIp));
    }

    private static String normalizeEmail(String email) {
        return Optional.ofNullable(email)
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse("");
    }

    private static String normalizeAccessIp(String accessIp) {
        return Optional.ofNullable(accessIp)
                .map(String::trim)
                .orElse("");
    }
}
