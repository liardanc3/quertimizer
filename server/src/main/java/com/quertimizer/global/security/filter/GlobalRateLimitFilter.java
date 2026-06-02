package com.quertimizer.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quertimizer.global.exception.ApiExceptionHandler;
import com.quertimizer.global.exception.DomainRuleViolationException;
import com.quertimizer.global.exception.GlobalFailReason;
import com.quertimizer.global.ratelimit.InMemoryGlobalRateLimiter;
import com.quertimizer.global.util.ClientIpResolver;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static com.quertimizer.global.ratelimit.GlobalRateLimitRules.HTTP_ADMIN_LONG_LIMIT;
import static com.quertimizer.global.ratelimit.GlobalRateLimitRules.HTTP_ADMIN_SHORT_LIMIT;
import static com.quertimizer.global.ratelimit.GlobalRateLimitRules.HTTP_ANONYMOUS_LONG_LIMIT;
import static com.quertimizer.global.ratelimit.GlobalRateLimitRules.HTTP_ANONYMOUS_SHORT_LIMIT;
import static com.quertimizer.global.ratelimit.GlobalRateLimitRules.HTTP_AUTHENTICATED_LONG_LIMIT;
import static com.quertimizer.global.ratelimit.GlobalRateLimitRules.HTTP_AUTHENTICATED_SHORT_LIMIT;
import static com.quertimizer.global.ratelimit.GlobalRateLimitRules.HTTP_LONG_WINDOW;
import static com.quertimizer.global.ratelimit.GlobalRateLimitRules.HTTP_SHORT_WINDOW;

@Slf4j
@Component
@RequiredArgsConstructor
public class GlobalRateLimitFilter extends OncePerRequestFilter {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    private final ObjectMapper objectMapper;
    private final ClientIpResolver clientIpResolver;
    private final InMemoryGlobalRateLimiter rateLimiter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // HTTP 요청 주체 기준 rate limit 적용
        RateLimitTarget target = resolveTarget(request);
        try {
            rateLimiter.validateAndRecord(
                    "http:short:" + target.key, HTTP_SHORT_WINDOW, target.shortLimit,
                    "http:long:" + target.key, HTTP_LONG_WINDOW, target.longLimit
            );
        } catch (DomainRuleViolationException exception) {
            log.warn("[RateLimit] HTTP 요청 제한 method={} path={} key={}",
                    request.getMethod(), request.getRequestURI(), target.key);
            writeTooManyRequests(response);
            return;
        }

        // 제한 대상이 아니면 다음 필터 또는 실제 endpoint 로직으로 전달
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 프리플라이트와 WebSocket handshake는 별도 흐름으로 처리
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || "/ws/session".equals(request.getRequestURI());
    }

    private RateLimitTarget resolveTarget(HttpServletRequest request) {
        // 인증 사용자와 비로그인 IP 기준 제한 key 결정
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isAuthenticated(authentication)) {
            boolean admin = isAdmin(authentication);
            return new RateLimitTarget(
                    "user:" + normalize(authentication.getName()),
                    admin ? HTTP_ADMIN_SHORT_LIMIT : HTTP_AUTHENTICATED_SHORT_LIMIT,
                    admin ? HTTP_ADMIN_LONG_LIMIT : HTTP_AUTHENTICATED_LONG_LIMIT
            );
        }

        return new RateLimitTarget(
                "ip:" + normalize(clientIpResolver.resolve(request)),
                HTTP_ANONYMOUS_SHORT_LIMIT,
                HTTP_ANONYMOUS_LONG_LIMIT
        );
    }

    private boolean isAuthenticated(Authentication authentication) {
        // 익명 사용자를 제외한 인증 여부 확인
        return authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName());
    }

    private boolean isAdmin(Authentication authentication) {
        // 관리자 권한 여부 확인
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> ROLE_ADMIN.equals(authority.getAuthority()));
    }

    private void writeTooManyRequests(HttpServletResponse response) throws IOException {
        // rate limit 초과 응답을 공용 JSON 형식으로 변환
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(
                response.getWriter(),
                ApiExceptionHandler.ExceptionResponse.reason(GlobalFailReason.REQUEST_RATE_LIMIT_DETAIL.getMessage())
        );
    }

    private String normalize(String value) {
        // 제한 key에 사용할 수 있도록 빈 식별값 정리
        return value == null || value.isBlank() ? "unknown" : value.trim();
    }

    private static final class RateLimitTarget {

        private final String key;
        private final int shortLimit;
        private final int longLimit;

        private RateLimitTarget(String key, int shortLimit, int longLimit) {
            this.key = key;
            this.shortLimit = shortLimit;
            this.longLimit = longLimit;
        }
    }
}
