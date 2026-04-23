package com.quertimizer.submit.application.result;

public record SubmitHistoryListItemResult(String submitId,
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
