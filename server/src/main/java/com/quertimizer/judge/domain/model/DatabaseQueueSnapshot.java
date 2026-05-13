package com.quertimizer.judge.domain.model;

import com.quertimizer.judge.domain.model.DbmsType;

import lombok.Data;

@Data
public class DatabaseQueueSnapshot {

    private final DbmsType dbmsType;
    private final int waitingCount;
}
