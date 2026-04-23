package com.quertimizer.dashboard.application.usecase;

import com.quertimizer.auth.application.service.AuthService;
import com.quertimizer.dashboard.application.result.DashboardResult;
import com.quertimizer.dashboard.application.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetDashboard {

    private final DashboardService dashboardService;
    private final AuthService authService;

    public DashboardResult execute(Authentication authentication) {
        return dashboardService.getDashboard(resolveCurrentHandle(authentication));
    }

    private String resolveCurrentHandle(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getName())) {
            return null;
        }

        return authService.resolveCurrentHandle(authentication.getName());
    }

}
