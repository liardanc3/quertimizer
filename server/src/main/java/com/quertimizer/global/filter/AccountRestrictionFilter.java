package com.quertimizer.global.filter;

import com.quertimizer.auth.application.service.AccountRestrictionService;
import com.quertimizer.auth.domain.policy.LoginPolicy;
import com.quertimizer.global.exception.BusinessException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AccountRestrictionFilter extends OncePerRequestFilter {

    private final AccountRestrictionService accountRestrictionService;
    private final LoginPolicy loginPolicy;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 차단된 IP는 인증 여부와 상관없이 즉시 요청을 거부한다.
        String clientIp = resolveClientIp(request);
        if (accountRestrictionService.isBlockedIp(clientIp)) {
            response.sendError(HttpStatus.FORBIDDEN.value());
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {

            // 인증된 사용자는 차단 계정 여부를 다시 확인
            if (isBlockedUser(authentication)) {
                response.sendError(HttpStatus.FORBIDDEN.value());
                return;
            }
        }

        // 차단 대상이 아니면 다음 필터 또는 실제 endpoint 로직으로 넘긴다.
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 프리플라이트와 logout은 필터에서 별도 차단/기록을 하지 않는다.
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || "/logout".equals(request.getRequestURI());
    }

    private boolean isBlockedUser(Authentication authentication) {
        // 차단 사용자 여부 확인
        try {
            loginPolicy.validateBlockedUser(authentication.getName());
            return false;

        } catch (BusinessException exception) {
            return true;
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        // 프록시 환경에서는 X-Forwarded-For의 첫 번째 IP를 실제 클라이언트 IP로 간주한다.
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

}
