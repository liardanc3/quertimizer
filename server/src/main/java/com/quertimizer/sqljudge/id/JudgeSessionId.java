package com.quertimizer.sqljudge.id;

import java.util.Objects;

/**
 * Identifies an external user session or request session.
 */
public class JudgeSessionId {

    private final String value;

    /**
     * Creates a SQL execution session ID.
     *
     * @param value session ID value
     */
    public JudgeSessionId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JudgeSessionId value must not be blank");
        }

        this.value = value;
    }

    /**
     * Returns the session ID value.
     *
     * @return session ID value
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

        if (!(object instanceof JudgeSessionId other)) {
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
