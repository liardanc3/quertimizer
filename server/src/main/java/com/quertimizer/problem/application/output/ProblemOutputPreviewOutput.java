package com.quertimizer.problem.application.output;

import lombok.Data;

import java.util.List;

@Data
public class ProblemOutputPreviewOutput {

    private final List<String> columns;
    private final List<List<String>> rows;
    private final long rowCount;
    private final int visibleRows;
    private final int rowLimit;

    public static ProblemOutputPreviewOutput from(ProblemJudgeExecutionResult output, int rowLimit) {
        return new ProblemOutputPreviewOutput(
                output.getColumns(), output.getRows(),
                output.getRowCount(), output.getRows().size(), rowLimit
        );
    }
}
