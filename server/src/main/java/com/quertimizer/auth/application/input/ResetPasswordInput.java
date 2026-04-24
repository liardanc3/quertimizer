package com.quertimizer.auth.application.input;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Locale;
import java.util.Optional;

@Data
@AllArgsConstructor
public class ResetPasswordInput {

    private final String email;
    private final String password;

    public static ResetPasswordInput of(String email, String password) {
        // 정규화된 비밀번호 재설정 입력 생성
        return new ResetPasswordInput(normalizeEmail(email), password);
    }

    private static String normalizeEmail(String email) {
        // 이메일 정규화
        return Optional.ofNullable(email)
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse("");
    }

}
