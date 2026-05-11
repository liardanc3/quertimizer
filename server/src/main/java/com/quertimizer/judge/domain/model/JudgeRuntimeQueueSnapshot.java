package com.quertimizer.judge.domain.model;

import lombok.Data;

@Data
public class JudgeRuntimeQueueSnapshot {

    private final DbmsType dbmsType;
    private final int waitingCount;
}
