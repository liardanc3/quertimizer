package com.quertimizer.judge.infrastructure.runtime;

import java.util.Objects;

public class ProvisionedRuntimeEnvironment {

    private final RuntimeEnvironment runtimeEnvironment;
    private final String provisionerName;

    public ProvisionedRuntimeEnvironment(RuntimeEnvironment runtimeEnvironment, String provisionerName) {
        this.runtimeEnvironment = Objects.requireNonNull(runtimeEnvironment, "runtimeEnvironment must not be null");
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
            throw new IllegalArgumentException(name + " must not be blank");
        }

        return value.trim();
    }
}
