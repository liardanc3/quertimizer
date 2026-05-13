package com.quertimizer.judge.application.model;

import lombok.Data;

import java.util.Objects;

@Data
public class EnvironmentName {

    private final String value;

    public EnvironmentName(String value) {
        this.value = value.trim();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof EnvironmentName other)) {
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
