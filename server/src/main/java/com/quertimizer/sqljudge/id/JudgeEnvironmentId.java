package com.quertimizer.sqljudge.id;

import java.util.Objects;

/**
 * Identifies a SQL execution environment.
 */
public class JudgeEnvironmentId {

    private final String value;

    /**
     * Creates a SQL execution environment ID.
     *
     * @param value environment ID value
     */
    public JudgeEnvironmentId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JudgeEnvironmentId value must not be blank");
        }

        this.value = value;
    }

    /**
     * Returns the environment ID value.
     *
     * @return environment ID value
     */
    public String getValue() {
        return value;
    }

    /**
     * Compares this ID with another object.
     *
     * @param object object to compare
     * @return true when both IDs have the same value
     */
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

    /**
     * Returns the hash code for this ID.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * Returns a string representation of this ID.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return value;
    }
}
