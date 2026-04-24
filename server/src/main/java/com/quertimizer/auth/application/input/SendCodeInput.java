package com.quertimizer.auth.application.input;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Locale;
import java.util.Optional;

@Data
@AllArgsConstructor
public class SendCodeInput {

    private final String email;

    public static SendCodeInput of(String email) {
        // 정규화된 인증코드 발송 입력 생성
        return new SendCodeInput(normalizeEmail(email));
    }

    private static String normalizeEmail(String email) {
        // 이메일 정규화
        return Optional.ofNullable(email)
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .orElse("");
    }
}
