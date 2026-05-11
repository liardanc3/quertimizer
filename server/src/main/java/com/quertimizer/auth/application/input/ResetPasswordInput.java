package com.quertimizer.auth.application.input;

import lombok.Data;

import java.util.Locale;
import java.util.Optional;

@Data
public class ResetPasswordInput {

    private final String email;
    private final String password;
    private final String clientIp;

    public static ResetPasswordInput of(String email, String password, String clientIp) {
        // 정규화된 비밀번호 재설정 입력 생성
        return new ResetPasswordInput(normalizeEmail(email), password, normalizeClientIp(clientIp));
    }

    private static String normalizeEmail(String email) {
        // 이메일 정규화
        return Optional.ofNullable(email)
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse("");
    }

    private static String normalizeClientIp(String clientIp) {
        return Optional.ofNullable(clientIp)
                .map(String::trim)
                .orElse("");
    }

}
