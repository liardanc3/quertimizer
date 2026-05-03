package com.quertimizer.judge.adapter.out.execution;

import java.util.Objects;

public class RuntimeEnvironmentName {

    private final String value;

    public RuntimeEnvironmentName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("런타임 실행 환경 이름이 비어 있다.");
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
