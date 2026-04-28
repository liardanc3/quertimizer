package com.quertimizer.dashboard.application.usecase;

import com.quertimizer.dashboard.application.output.DashboardOutput;
import com.quertimizer.dashboard.application.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetDashboard {

    private final DashboardService dashboardService;

    /**
     * 로그인 여부에 맞는 대시보드 데이터를 조회한다.
     *
     * @param currentHandle 현재 사용자 handle
     */
    public DashboardOutput execute(String currentHandle) {
        return dashboardService.getDashboard(currentHandle);
    }
}
