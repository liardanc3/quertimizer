package com.quertimizer.dashboard.application.port.in;

import com.quertimizer.dashboard.application.output.DashboardOutput;

public interface GetDashboardUseCase {

    DashboardOutput execute(String currentHandle);
}
