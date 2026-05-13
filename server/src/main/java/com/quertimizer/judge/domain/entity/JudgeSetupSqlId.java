package com.quertimizer.judge.domain.entity;

import lombok.Data;

import java.util.Objects;

@Data
public class JudgeSetupSqlId {

    private final String value;

    public JudgeSetupSqlId(String value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof JudgeSetupSqlId other)) {
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
