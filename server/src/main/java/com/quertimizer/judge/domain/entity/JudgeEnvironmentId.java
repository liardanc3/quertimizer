package com.quertimizer.judge.domain.entity;

import java.util.Objects;

public class JudgeEnvironmentId {

    private final String value;

    public JudgeEnvironmentId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("judge 실행 환경 ID가 비어 있다.");
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

        if (!(object instanceof JudgeEnvironmentId other)) {
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
