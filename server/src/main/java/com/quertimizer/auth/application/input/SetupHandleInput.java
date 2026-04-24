package com.quertimizer.auth.application.input;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Optional;

@Data
@AllArgsConstructor
public class SetupHandleInput {

    private final String authenticatedEmail;
    private final String handle;

    public static SetupHandleInput of(String authenticatedEmail, String handle) {
        // 정규화된 Handle 설정 입력 생성
        return new SetupHandleInput(normalizeAuthenticatedEmail(authenticatedEmail), normalizeHandle(handle));
    }

    private static String normalizeAuthenticatedEmail(String authenticatedEmail) {
        // 인증 이메일 정규화
        return Optional.ofNullable(authenticatedEmail)
                .map(String::trim)
                .orElse("");
    }

    private static String normalizeHandle(String handle) {
        // Handle 정규화
        return Optional.ofNullable(handle)
                .map(String::trim)
                .orElse("");
    }
}
