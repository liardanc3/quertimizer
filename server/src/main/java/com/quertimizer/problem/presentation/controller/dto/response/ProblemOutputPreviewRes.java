package com.quertimizer.problem.presentation.controller.dto.response;

import com.quertimizer.problem.application.output.ProblemOutputPreviewOutput;
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
        return new ProblemOutputPreviewRes(output.getColumns(), output.getRows(), output.getRowCount());
    }
}
