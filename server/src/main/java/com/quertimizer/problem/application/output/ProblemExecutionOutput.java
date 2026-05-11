package com.quertimizer.problem.application.output;

import lombok.Data;

import java.util.List;

@Data
public class ProblemExecutionOutput {

    private final String problemId;
    private final String mode;
    private final String message;
    private final List<String> columns;
    private final List<List<String>> rows;
    private final List<String> planLines;
    private final long rowCount;
    private final Integer currentPage;
    private final Integer pageSize;
    private final long executionTimeMs;
    private final Double cost;
}
