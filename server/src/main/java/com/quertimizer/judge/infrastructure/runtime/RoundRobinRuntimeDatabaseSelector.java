package com.quertimizer.judge.infrastructure.runtime;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinRuntimeDatabaseSelector implements RuntimeDatabaseSelector {

    private final AtomicInteger cursor = new AtomicInteger();

    @Override
    public int selectStartIndex(List<RuntimeDatabase> candidates) {
        Objects.requireNonNull(candidates, "candidates must not be null");

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("candidates must not be empty");
        }

        return Math.floorMod(cursor.getAndIncrement(), candidates.size());
    }
}
