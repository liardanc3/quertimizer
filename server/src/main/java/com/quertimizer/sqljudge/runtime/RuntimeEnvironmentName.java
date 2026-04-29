package com.quertimizer.sqljudge.runtime;

import java.util.Objects;

/**
 * Represents an internal runtime environment name.
 */
public class RuntimeEnvironmentName {

    private final String value;

    /**
     * Creates an internal runtime environment name.
     *
     * @param value runtime environment name value
     */
    public RuntimeEnvironmentName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("RuntimeEnvironmentName value must not be blank");
        }

        this.value = value;
    }

    /**
     * Returns the runtime environment name value.
     *
     * @return runtime environment name value
     */
    public String getValue() {
        return value;
    }

    /**
     * Compares this name with another object.
     *
     * @param object object to compare
     * @return true when both names have the same value
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof RuntimeEnvironmentName other)) {
            return false;
        }

        return Objects.equals(value, other.value);
    }

    /**
     * Returns the hash code for this name.
     *
     * @return hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    /**
     * Returns a string representation of this name.
     *
     * @return string representation
     */
    @Override
    public String toString() {
        return value;
    }
}
