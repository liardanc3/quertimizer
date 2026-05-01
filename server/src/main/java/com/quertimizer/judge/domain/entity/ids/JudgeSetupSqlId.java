package com.quertimizer.judge.domain.entity.ids;

import java.util.Objects;

public class JudgeSetupSqlId {

    private final String value;

    public JudgeSetupSqlId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JudgeSetupSqlId value must not be blank");
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
