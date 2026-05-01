package com.quertimizer.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    private static final List<String> ALLOWED_ORIGIN_PATTERNS = List.of(
            "http://localhost:*", "http://127.0.0.1:*",
            "https://quertimizer.com", "https://www.quertimizer.com"
    );

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        // 로컬과 운영 프론트엔드에서 인증 쿠키를 포함한 API 호출 허용
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(ALLOWED_ORIGIN_PATTERNS);
        configuration.setAllowedMethods(List.of("*"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        // 모든 API 경로에 CORS 설정 등록
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
