package com.quertimizer.problem.application.output;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ProblemOutputPreviewOutput {

    private final List<String> columns;
    private final List<List<String>> rows;
    private final long rowCount;

    public static ProblemOutputPreviewOutput from(ProblemJudgeExecutionResult output) {
        return new ProblemOutputPreviewOutput(output.getColumns(), output.getRows(), output.getRowCount());
    }
}
