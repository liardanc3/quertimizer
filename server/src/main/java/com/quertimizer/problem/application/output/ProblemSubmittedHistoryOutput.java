package com.quertimizer.problem.application.output;

public record ProblemSubmittedHistoryOutput(String dbms,
                                            String handle,
                                            long executionPlanElement,
                                            long executionTimeMs,
                                            double cost) {
}
