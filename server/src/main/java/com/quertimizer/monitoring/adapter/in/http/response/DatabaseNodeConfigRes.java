package com.quertimizer.monitoring.adapter.in.http.response;

import com.quertimizer.judge.application.output.DatabaseNodeConfigOutput;
import lombok.Data;

@Data
public class DatabaseNodeConfigRes {

    private final String databaseId;
    private final String databaseName;
    private final String dbmsType;
    private final String dbmsLabel;
    private final boolean enabled;
    private final int maxConcurrency;
    private final String updatedAt;

    public static DatabaseNodeConfigRes from(DatabaseNodeConfigOutput output) {
        return new DatabaseNodeConfigRes(
                output.getDatabaseId(), output.getDatabaseName(),
                output.getDbmsType().getValue(), output.getDbmsType().getLabel(),
                output.isEnabled(), output.getMaxConcurrency(),
                output.getUpdatedAt() != null ? output.getUpdatedAt().toString() : ""
        );
    }
}
