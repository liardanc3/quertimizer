package com.quertimizer.monitoring.adapter.in.web.realtime.dto;

import com.quertimizer.monitoring.adapter.in.web.response.SystemResourceRes;
import lombok.Data;

@Data
public class MonitoringResourceSocketRes {

    private final String type;
    private final SystemResourceRes resource;
    private final String message;

    public static MonitoringResourceSocketRes success(SystemResourceRes resource) {
        return new MonitoringResourceSocketRes("monitoring.resources.result", resource, null);
    }

    public static MonitoringResourceSocketRes error(String message) {
        return new MonitoringResourceSocketRes("monitoring.resources.error", null, message);
    }
}
