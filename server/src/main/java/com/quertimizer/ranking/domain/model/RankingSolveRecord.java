package com.quertimizer.ranking.domain.model;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class RankingSolveRecord {

    private final String problemId;
    private final String handle;
    private final DbmsType dbmsType;
    private final long executionTimeMs;
    private final double cost;
    private final LocalDateTime submittedAt;

}
