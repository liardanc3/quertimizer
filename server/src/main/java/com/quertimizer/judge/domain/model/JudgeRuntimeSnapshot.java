package com.quertimizer.judge.domain.model;

import lombok.Data;

import java.util.List;

@Data
public class JudgeRuntimeSnapshot {

    private final List<JudgeRuntimeNodeSnapshot> nodes;
    private final List<JudgeRuntimeQueueSnapshot> queues;
    private final int totalWaitingCount;
    private final int totalRunningCount;
}
