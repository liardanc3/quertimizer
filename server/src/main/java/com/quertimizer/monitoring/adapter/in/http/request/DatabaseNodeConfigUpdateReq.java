package com.quertimizer.monitoring.adapter.in.http.request;

import com.quertimizer.judge.application.input.DatabaseNodeConfigUpdateInput;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class DatabaseNodeConfigUpdateReq {

    @NotNull
    private Boolean enabled;

    @Min(1)
    private int maxConcurrency;

    public DatabaseNodeConfigUpdateInput toInput(String databaseId) {
        return new DatabaseNodeConfigUpdateInput(databaseId, Boolean.TRUE.equals(enabled), maxConcurrency);
    }
}
