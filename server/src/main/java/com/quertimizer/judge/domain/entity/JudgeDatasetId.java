package com.quertimizer.judge.domain.entity;

import lombok.Data;

import java.util.Objects;

@Data
public class JudgeDatasetId {

    private final Long value;

    public JudgeDatasetId(Long value) {
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
        return String.valueOf(value);
    }
}
