package com.quertimizer.problem.application.output;

import lombok.Data;

import java.util.List;

@Data
public class ProblemOutputExampleOutput {

    private final int rowLimit;
    private final List<String> columns;
    private final List<List<String>> rows;
    private final long totalRows;
    private final int visibleRows;
}
