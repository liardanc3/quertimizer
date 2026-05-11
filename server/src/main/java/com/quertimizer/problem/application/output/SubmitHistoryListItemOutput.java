package com.quertimizer.problem.application.output;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class SubmitHistoryListItemOutput {

    private final String submitId;
    private final String handle;
    private final String dbms;
    private final String problemId;
    private final String submittedAt;
    private final boolean success;
    private final String message;
    private final String submittedSql;
    private final double cost;
    private final long executionPlanElement;
}
