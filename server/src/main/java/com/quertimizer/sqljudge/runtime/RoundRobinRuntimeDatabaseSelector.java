package com.quertimizer.sqljudge.runtime;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Selects runtime databases with a round-robin start index.
 */
public class RoundRobinRuntimeDatabaseSelector implements RuntimeDatabaseSelector {

    private final AtomicInteger cursor = new AtomicInteger();

    /**
     * Selects the next round-robin start index.
     *
     * @param candidates runtime database candidates
     * @return selected start index
     */
    @Override
    public int selectStartIndex(List<RuntimeDatabase> candidates) {
        Objects.requireNonNull(candidates, "candidates must not be null");

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates must not be empty");
        }

        return Math.floorMod(cursor.getAndIncrement(), candidates.size());
    }
}
