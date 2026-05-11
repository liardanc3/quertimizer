package com.quertimizer.monitoring.application.port.out;

import com.quertimizer.monitoring.application.input.MonitoringLogSearchInput;
import com.quertimizer.monitoring.application.output.ServerLogOutput;

public interface ServerLogPort {

    ServerLogOutput readLogs(MonitoringLogSearchInput input);
}
