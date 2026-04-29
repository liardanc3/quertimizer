package com.quertimizer.sqljudge.id;

import java.util.Objects;

/**
 * Identifies a registered reference SQL definition.
 */
public class JudgeReferenceId {

    private final String value;

    /**
     * Creates a registered reference SQL ID.
     *
     * @param value reference SQL ID value
     */
    public JudgeReferenceId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JudgeReferenceId value must not be blank");
        }

        this.value = value;
    }

    /**
     * Returns the reference SQL ID value.
     *
     * @return reference SQL ID value
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

        if (!(object instanceof JudgeReferenceId other)) {
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
