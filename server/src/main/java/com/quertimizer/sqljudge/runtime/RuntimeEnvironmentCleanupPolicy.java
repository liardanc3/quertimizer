package com.quertimizer.sqljudge.runtime;

import java.time.Duration;
import java.util.Objects;

/**
 * Represents cleanup policy for sql-judge runtime environments.
 */
public class RuntimeEnvironmentCleanupPolicy {

    private final Duration inactiveTimeout;
    private final boolean cleanupResidualOnStart;

    /**
     * Creates a runtime environment cleanup policy.
     *
     * @param inactiveTimeout inactive environment timeout
     * @param cleanupResidualOnStart whether residual environments should be cleaned on start
     */
    public RuntimeEnvironmentCleanupPolicy(Duration inactiveTimeout, boolean cleanupResidualOnStart) {
        this.inactiveTimeout = Objects.requireNonNull(inactiveTimeout, "inactiveTimeout must not be null");
        this.cleanupResidualOnStart = cleanupResidualOnStart;
    }

    /**
     * Creates a default cleanup policy.
     *
     * @return default cleanup policy
     */
    public static RuntimeEnvironmentCleanupPolicy defaults() {
        return new RuntimeEnvironmentCleanupPolicy(Duration.ofMinutes(30), true);
    }

    /**
     * Returns the inactive environment timeout.
     *
     * @return inactive environment timeout
     */
    public Duration getInactiveTimeout() {
        return inactiveTimeout;
    }

    /**
     * Returns whether residual environments should be cleaned on start.
     *
     * @return true when residual environments should be cleaned on start
     */
    public boolean isCleanupResidualOnStart() {
        return cleanupResidualOnStart;
    }
}
