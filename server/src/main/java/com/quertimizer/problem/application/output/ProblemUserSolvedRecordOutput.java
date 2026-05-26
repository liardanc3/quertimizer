package com.quertimizer.problem.application.output;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProblemUserSolvedRecordOutput {

    private final String problemId;
    private final String title;
    private final String dbms;
    private final long executionTimeMs;
    private final double cost;
    private final LocalDateTime submittedAt;
}
