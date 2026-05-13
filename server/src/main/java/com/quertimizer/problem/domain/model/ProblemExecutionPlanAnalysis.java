package com.quertimizer.problem.domain.model;

import lombok.Data;

import java.util.List;

@Data
public class ProblemExecutionPlanAnalysis {

    private final long executionPlanElement;
    private final List<String> summaryLines;

    public ProblemExecutionPlanAnalysis(long executionPlanElement, List<String> summaryLines) {
        this.executionPlanElement = executionPlanElement;
        this.summaryLines = summaryLines != null ? List.copyOf(summaryLines) : List.of();
    }
}
