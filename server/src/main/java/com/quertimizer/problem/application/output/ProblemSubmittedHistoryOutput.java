package com.quertimizer.problem.application.output;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class ProblemSubmittedHistoryOutput {

    private final String dbms;
    private final String handle;
    private final long executionPlanElement;
    private final long executionTimeMs;
    private final double cost;
}
