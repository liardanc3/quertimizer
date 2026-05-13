package com.quertimizer.monitoring.adapter.in.websocket.dto;

import com.quertimizer.monitoring.adapter.in.http.response.DatabaseStatusRes;
import lombok.Data;

@Data
public class MonitoringDatabaseStatusSocketRes {

    private final String type;
    private final DatabaseStatusRes status;
    private final String message;

    public static MonitoringDatabaseStatusSocketRes success(DatabaseStatusRes status) {
        return new MonitoringDatabaseStatusSocketRes("monitoring.database-status.result", status, null);
    }

    public static MonitoringDatabaseStatusSocketRes error(String message) {
        return new MonitoringDatabaseStatusSocketRes("monitoring.database-status.error", null, message);
    }
}
