package com.quertimizer.auth.application.input;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Locale;
import java.util.Optional;

@Data
@AllArgsConstructor
public class VerifyCodeInput {

    private final String email;
    private final String code;

    public static VerifyCodeInput of(String email, String code) {
        // 정규화된 인증코드 확인 입력 생성
        return new VerifyCodeInput(normalizeEmail(email), normalizeCode(code));
    }

    private static String normalizeEmail(String email) {
        // 이메일 정규화
        return Optional.ofNullable(email)
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse("");
    }

    private static String normalizeCode(String code) {
        // 인증코드 정규화
        return Optional.ofNullable(code)
                .map(String::trim)
                .map(value -> value.toUpperCase(Locale.ROOT))
                .orElse("");
    }
}
