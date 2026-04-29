package com.quertimizer.sqljudge.id;

import java.util.Objects;

/**
 * Identifies a registered setup SQL bundle.
 */
public class JudgeSetupSqlId {

    private final String value;

    /**
     * Creates a registered setup SQL bundle ID.
     *
     * @param value setup SQL bundle ID value
     */
    public JudgeSetupSqlId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JudgeSetupSqlId value must not be blank");
        }

        this.value = value;
    }

    /**
     * Returns the setup SQL bundle ID value.
     *
     * @return setup SQL bundle ID value
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

        if (!(object instanceof JudgeSetupSqlId other)) {
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
