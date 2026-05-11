package com.quertimizer.monitoring.adapter.in.web.realtime.dto;

import com.quertimizer.monitoring.adapter.in.web.response.DbRuntimeRes;
import lombok.Data;

@Data
public class MonitoringDbRuntimeSocketRes {

    private final String type;
    private final DbRuntimeRes runtime;
    private final String message;

    public static MonitoringDbRuntimeSocketRes success(DbRuntimeRes runtime) {
        return new MonitoringDbRuntimeSocketRes("monitoring.db-runtime.result", runtime, null);
    }

    public static MonitoringDbRuntimeSocketRes error(String message) {
        return new MonitoringDbRuntimeSocketRes("monitoring.db-runtime.error", null, message);
    }
}
