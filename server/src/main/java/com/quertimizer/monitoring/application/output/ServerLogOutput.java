package com.quertimizer.monitoring.application.output;

import com.quertimizer.monitoring.domain.model.MonitoringLogLevel;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ServerLogOutput {

    private final MonitoringLogLevel level;
    private final LocalDate date;
    private final boolean exists;
    private final List<String> lines;
}
