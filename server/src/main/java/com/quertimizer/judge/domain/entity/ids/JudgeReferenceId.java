package com.quertimizer.judge.domain.entity.ids;

import java.util.Objects;

public class JudgeReferenceId {

    private final String value;

    public JudgeReferenceId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JudgeReferenceId value must not be blank");
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

        if (!(object instanceof JudgeReferenceId other)) {
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
