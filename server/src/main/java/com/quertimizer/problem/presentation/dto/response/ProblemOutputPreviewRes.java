package com.quertimizer.problem.presentation.dto.response;

import com.quertimizer.judge.application.output.ProblemOutputPreviewOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ProblemOutputPreviewRes {

    private final List<String> columns;
    private final List<List<String>> rows;
    private final long rowCount;

    public static ProblemOutputPreviewRes from(ProblemOutputPreviewOutput output) {
        return new ProblemOutputPreviewRes(output.columns(), output.rows(), output.rowCount());
    }
}
