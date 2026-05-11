package com.quertimizer.judge.domain.entity;

import lombok.Getter;

import java.util.Objects;

@Getter
public class JudgeDatasetId {

    private final String value;

    public JudgeDatasetId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("judge 데이터셋 ID가 비어 있습니다.");
        }

        this.value = value;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof JudgeDatasetId other)) {
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
