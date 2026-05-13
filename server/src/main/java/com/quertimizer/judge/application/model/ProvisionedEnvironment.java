package com.quertimizer.judge.application.model;

import lombok.Data;

@Data
public class ProvisionedEnvironment {

    private final ExecutionEnvironment executionEnvironment;

    public ProvisionedEnvironment(ExecutionEnvironment executionEnvironment) {
        this.executionEnvironment = executionEnvironment;
    }
}
