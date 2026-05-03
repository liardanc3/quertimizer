package com.quertimizer.judge.adapter.out.execution;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinRuntimeDatabaseSelector implements RuntimeDatabaseSelector {

    private final AtomicInteger cursor = new AtomicInteger();

    @Override
    public int selectStartIndex(List<RuntimeDatabase> candidates) {
        Objects.requireNonNull(candidates, "필수 값이 없다.");

        if (candidates.isEmpty()) {
            throw new IllegalArgumentException("후보 목록이 비어 있다.");
        }

        return Math.floorMod(cursor.getAndIncrement(), candidates.size());
    }
}
