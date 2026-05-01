package com.quertimizer.problem.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class ProblemSubmittedHistoryOutput {

    private final String dbms;
    private final String handle;
    private final long executionPlanElement;
    private final long executionTimeMs;
    private final double cost;
}
