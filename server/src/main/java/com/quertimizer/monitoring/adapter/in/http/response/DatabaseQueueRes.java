package com.quertimizer.monitoring.adapter.in.http.response;

import com.quertimizer.monitoring.application.output.DatabaseQueueOutput;
import lombok.Data;

@Data
public class DatabaseQueueRes {

    private final String dbmsType;
    private final String dbmsLabel;
    private final int waitingCount;

    public static DatabaseQueueRes from(DatabaseQueueOutput output) {
        return new DatabaseQueueRes(output.getDbmsType().getValue(), output.getDbmsType().getLabel(), output.getWaitingCount());
    }
}
