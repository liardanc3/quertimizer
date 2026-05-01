package com.quertimizer.judge.infrastructure.runtime;

import java.util.Objects;

public class RuntimeEnvironmentName {

    private final String value;

    public RuntimeEnvironmentName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("RuntimeEnvironmentName value must not be blank");
        }

        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof RuntimeEnvironmentName other)) {
            return false;
        }

        return Objects.equals(value, other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
