package com.quertimizer.filter;

import com.quertimizer.service.AccountRestrictionService;
import com.quertimizer.service.UserAccessService;
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
    private final UserAccessService userAccessService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String clientIp = resolveClientIp(request);
        if (accountRestrictionService.isBlockedIp(clientIp)) {
            response.sendError(HttpStatus.FORBIDDEN.value());
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            String currentUserId = userAccessService.resolveCurrentUserId(authentication.getName());
            if (accountRestrictionService.isBlockedUser(currentUserId)) {
                response.sendError(HttpStatus.FORBIDDEN.value());
                return;
            }

            userAccessService.recordAccess(authentication.getName(), clientIp);
        }

        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || "/logout".equals(request.getRequestURI());
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        return request.getRemoteAddr();
    }

}
