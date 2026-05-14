package com.quertimizer.auth.application.input;

import lombok.Data;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Data
public class SocialLoginInput {

    private final String provider;
    private final Map<String, Object> attributes;
    private final String accessIp;

    public static SocialLoginInput of(String provider, Map<String, Object> attributes, String accessIp) {
        // 정규화된 소셜 로그인 입력 생성
        return new SocialLoginInput(
                normalizeProvider(provider),
                normalizeAttributes(attributes),
                normalizeAccessIp(accessIp)
        );
    }

    private static String normalizeProvider(String provider) {
        // OAuth provider 정규화
        return Optional.ofNullable(provider)
                .map(String::trim)
                .orElse("");
    }

    private static String normalizeAccessIp(String accessIp) {
        // 접근 Ip 정규화
        return Optional.ofNullable(accessIp)
                .map(String::trim)
                .orElse("");
    }

    private static Map<String, Object> normalizeAttributes(Map<String, Object> attributes) {
        // GitHub OAuth attributes는 null value를 포함할 수 있으므로 null 보존 복사
        if (attributes == null || attributes.isEmpty()) {
            return Map.of();
        }

        return Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }
}
