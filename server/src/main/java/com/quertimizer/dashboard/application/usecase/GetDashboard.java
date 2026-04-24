package com.quertimizer.dashboard.application.usecase;

import com.quertimizer.dashboard.application.output.DashboardOutput;
import com.quertimizer.dashboard.application.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetDashboard {

    private final DashboardService dashboardService;

    public DashboardOutput execute(String currentHandle) {
        // 대시보드 데이터를 조회
        return dashboardService.getDashboard(currentHandle);
    }
}
