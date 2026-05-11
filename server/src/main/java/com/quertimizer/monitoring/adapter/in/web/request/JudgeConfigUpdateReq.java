package com.quertimizer.monitoring.adapter.in.web.request;

import com.quertimizer.monitoring.application.input.JudgeConfigUpdateInput;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class JudgeConfigUpdateReq {

    @NotNull
    private Boolean enabled;

    @Min(1)
    private int maxConcurrency;

    public JudgeConfigUpdateInput toInput(String databaseId) {
        return new JudgeConfigUpdateInput(databaseId, Boolean.TRUE.equals(enabled), maxConcurrency);
    }
}
