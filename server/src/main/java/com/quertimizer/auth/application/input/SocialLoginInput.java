package com.quertimizer.auth.application.input;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.Authentication;

import java.util.Optional;

@Data
@AllArgsConstructor
public class SocialLoginInput {

    private final Authentication authentication;
    private final String accessIp;

    public static SocialLoginInput of(Authentication authentication, String accessIp) {
        // 정규화된 소셜 로그인 입력 생성
        return new SocialLoginInput(authentication, normalizeAccessIp(accessIp));
    }

    private static String normalizeAccessIp(String accessIp) {
        // 접근 Ip 정규화
        return Optional.ofNullable(accessIp)
                .map(String::trim)
                .orElse("");
    }
}
