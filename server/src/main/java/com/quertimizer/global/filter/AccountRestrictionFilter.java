package com.quertimizer.global.filter;

import com.quertimizer.auth.application.service.AccountRestrictionService;
import com.quertimizer.auth.application.port.in.ValidateAuthenticatedUserAccessUseCase;
import com.quertimizer.global.exception.DomainRuleViolationException;
import com.quertimizer.global.support.ClientIpResolver;
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
    private final ValidateAuthenticatedUserAccessUseCase validateAuthenticatedUserAccess;
    private final ClientIpResolver clientIpResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // 차단된 IP는 인증 여부와 상관없이 즉시 요청 거부
        String clientIp = clientIpResolver.resolve(request);
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

        // 차단 대상이 아니면 다음 필터 또는 실제 endpoint 로직으로 전달
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // 프리플라이트와 logout은 필터에서 별도 차단/기록 생략
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || "/logout".equals(request.getRequestURI());
    }

    private boolean isBlockedUser(Authentication authentication) {
        // 차단 사용자 여부 확인
        try {
            validateAuthenticatedUserAccess.execute(authentication.getName());
            return false;
        } catch (DomainRuleViolationException exception) {
            return true;
        }
    }

}
