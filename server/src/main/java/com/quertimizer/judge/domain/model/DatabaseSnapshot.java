package com.quertimizer.judge.domain.model;

import lombok.Data;

import java.util.List;

@Data
public class DatabaseSnapshot {

    private final List<DatabaseNodeSnapshot> nodes;
    private final List<DatabaseQueueSnapshot> queues;
    private final int totalWaitingCount;
    private final int totalRunningCount;
}
