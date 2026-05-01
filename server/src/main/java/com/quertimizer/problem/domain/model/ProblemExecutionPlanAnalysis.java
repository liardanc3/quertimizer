package com.quertimizer.problem.domain.model;

import java.util.List;

public class ProblemExecutionPlanAnalysis {

    private final long executionPlanElement;
    private final List<String> summaryLines;

    public ProblemExecutionPlanAnalysis(long executionPlanElement, List<String> summaryLines) {
        this.executionPlanElement = executionPlanElement;
        this.summaryLines = summaryLines != null ? List.copyOf(summaryLines) : List.of();
    }

    public long getExecutionPlanElement() {
        return executionPlanElement;
    }

    public List<String> getSummaryLines() {
        return summaryLines;
    }
}
