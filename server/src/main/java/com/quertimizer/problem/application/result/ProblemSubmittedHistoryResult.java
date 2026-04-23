package com.quertimizer.problem.application.result;

public record ProblemSubmittedHistoryResult(String dbms,
                                            String handle,
                                            long executionPlanElement,
                                            long executionTimeMs,
                                            double cost) {
}
