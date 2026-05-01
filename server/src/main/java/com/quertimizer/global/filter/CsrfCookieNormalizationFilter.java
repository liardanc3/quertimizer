package com.quertimizer.global.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class CsrfCookieNormalizationFilter extends OncePerRequestFilter {

    private static final String PRODUCTION_API_HOST = "server.quertimizer.com";
    private static final String CSRF_COOKIE_NAME = "XSRF-TOKEN";
    private static final String CSRF_HEADER_NAME = "X-XSRF-TOKEN";
    private static final String EXPIRED_HOST_CSRF_COOKIE =
            "XSRF-TOKEN=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Path=/; Secure; SameSite=Lax";
    private static final String EXPIRED_API_DOMAIN_CSRF_COOKIE =
            "XSRF-TOKEN=; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Domain=server.quertimizer.com; Path=/; Secure; SameSite=Lax";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 운영 API host에 남아 있는 API host 전용 CSRF 쿠키 제거
        if (PRODUCTION_API_HOST.equalsIgnoreCase(request.getServerName())) {
            response.addHeader("Set-Cookie", EXPIRED_HOST_CSRF_COOKIE);
            response.addHeader("Set-Cookie", EXPIRED_API_DOMAIN_CSRF_COOKIE);
        }

        // 중복 CSRF 쿠키가 있으면 요청 header와 일치하는 쿠키를 우선 사용
        HttpServletRequest csrfCookieAdjustedRequest = adjustDuplicateCsrfCookie(request);

        // 다음 보안 필터 체인 실행
        filterChain.doFilter(csrfCookieAdjustedRequest, response);
    }

    private HttpServletRequest adjustDuplicateCsrfCookie(HttpServletRequest request) {
        // CSRF header가 없으면 기존 요청 그대로 사용
        String csrfHeader = request.getHeader(CSRF_HEADER_NAME);
        if (csrfHeader == null || csrfHeader.isBlank()) {
            return request;
        }

        // 요청 쿠키가 없으면 기존 요청 그대로 사용
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookies.length == 0) {
            return request;
        }

        // CSRF 쿠키 중 header와 값이 같은 쿠키를 선두로 이동
        List<Cookie> matchedCsrfCookies = new ArrayList<>();
        List<Cookie> remainingCookies = new ArrayList<>();
        int csrfCookieCount = 0;
        for (Cookie cookie : cookies) {
            if (!CSRF_COOKIE_NAME.equals(cookie.getName())) {
                remainingCookies.add(cookie);
                continue;
            }

            csrfCookieCount++;
            if (csrfHeader.equals(cookie.getValue())) {
                matchedCsrfCookies.add(cookie);
            } else {
                remainingCookies.add(cookie);
            }
        }

        // 중복 CSRF 쿠키가 아니거나 매칭 쿠키가 없으면 기존 요청 그대로 사용
        if (csrfCookieCount < 2 || matchedCsrfCookies.isEmpty()) {
            return request;
        }

        // CookieCsrfTokenRepository가 매칭 쿠키를 먼저 읽도록 요청 wrapper 생성
        matchedCsrfCookies.addAll(remainingCookies);
        Cookie[] adjustedCookies = matchedCsrfCookies.toArray(Cookie[]::new);
        return new HttpServletRequestWrapper(request) {

            @Override
            public Cookie[] getCookies() {
                return adjustedCookies.clone();
            }
        };
    }
}
