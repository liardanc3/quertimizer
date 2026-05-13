package com.quertimizer.judge.application.model;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class RoundRobinDatabaseSelector implements DatabaseSelector {

    private final AtomicInteger cursor = new AtomicInteger();

    @Override
    public int selectStartIndex(List<Database> candidates) {
        return Math.floorMod(cursor.getAndIncrement(), candidates.size());
    }
}
