package com.quertimizer.judge.domain.entity;

import java.util.Objects;

public class JudgeExecutionId {

    private final String value;

    public JudgeExecutionId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("judge 실행 ID가 비어 있습니다.");
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

        if (!(object instanceof JudgeExecutionId other)) {
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
