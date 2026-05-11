package com.quertimizer.monitoring.adapter.in.web.realtime.dto;

import com.quertimizer.monitoring.adapter.in.web.response.ServerLogRes;
import lombok.Data;

import java.util.List;

@Data
public class MonitoringLogSocketRes {

    private final String type;
    private final ServerLogRes log;
    private final List<String> lines;
    private final String message;

    public static MonitoringLogSocketRes snapshot(ServerLogRes log) {
        return new MonitoringLogSocketRes("monitoring.logs.snapshot", log, List.of(), null);
    }

    public static MonitoringLogSocketRes append(List<String> lines) {
        return new MonitoringLogSocketRes("monitoring.logs.append", null, lines, null);
    }

    public static MonitoringLogSocketRes error(String message) {
        return new MonitoringLogSocketRes("monitoring.logs.error", null, List.of(), message);
    }
}
