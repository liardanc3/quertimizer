package com.quertimizer.monitoring.adapter.in.web.response;

import com.quertimizer.monitoring.application.output.JudgeConfigOutput;
import lombok.Data;

@Data
public class JudgeConfigRes {

    private final String databaseId;
    private final String databaseName;
    private final String dbmsType;
    private final String dbmsLabel;
    private final boolean enabled;
    private final int maxConcurrency;
    private final String updatedAt;

    public static JudgeConfigRes from(JudgeConfigOutput output) {
        return new JudgeConfigRes(
                output.getDatabaseId(), output.getDatabaseName(),
                output.getDbmsType().getValue(), output.getDbmsType().getLabel(),
                output.isEnabled(), output.getMaxConcurrency(),
                output.getUpdatedAt() != null ? output.getUpdatedAt().toString() : ""
        );
    }
}
