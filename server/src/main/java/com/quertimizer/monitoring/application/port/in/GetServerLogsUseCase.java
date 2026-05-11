package com.quertimizer.monitoring.application.port.in;

import com.quertimizer.monitoring.application.input.MonitoringLogSearchInput;
import com.quertimizer.monitoring.application.output.ServerLogOutput;

public interface GetServerLogsUseCase {

    ServerLogOutput execute(MonitoringLogSearchInput input);
}
