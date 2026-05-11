package com.quertimizer.problem.application.output;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProblemRankingSolveRecordOutput {

    private final String problemId;
    private final String handle;
    private final DbmsType dbmsType;
    private final long executionTimeMs;
    private final double cost;
    private final LocalDateTime submittedAt;

}
