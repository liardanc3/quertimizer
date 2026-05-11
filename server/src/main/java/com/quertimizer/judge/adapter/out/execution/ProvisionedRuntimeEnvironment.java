package com.quertimizer.judge.adapter.out.execution;

import java.util.Objects;

public class ProvisionedRuntimeEnvironment {

    private final RuntimeEnvironment runtimeEnvironment;
    private final String provisionerName;

    public ProvisionedRuntimeEnvironment(RuntimeEnvironment runtimeEnvironment, String provisionerName) {
        this.runtimeEnvironment = Objects.requireNonNull(runtimeEnvironment, "필수 값이 없습니다.");
        this.provisionerName = requireText(provisionerName, "provisionerName");
    }

    public RuntimeEnvironment getRuntimeEnvironment() {
        return runtimeEnvironment;
    }

    public String getProvisionerName() {
        return provisionerName;
    }

    private String requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + "이 비어 있습니다.");
        }

        return value.trim();
    }
}
