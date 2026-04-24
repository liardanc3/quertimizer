package com.quertimizer.submit.application.output;

public record SubmitHistoryListItemOutput(String submitId,
                                          String handle,
                                          String dbms,
                                          String problemId,
                                          String submittedAt,
                                          boolean success,
                                          String message,
                                          String submittedSql,
                                          double cost,
                                          long executionPlanElement) {
}
