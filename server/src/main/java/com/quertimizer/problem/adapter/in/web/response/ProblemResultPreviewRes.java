package com.quertimizer.problem.adapter.in.web.response;

import com.quertimizer.problem.application.output.ProblemOutputPreviewOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ProblemResultPreviewRes {

    private final List<String> columns;
    private final List<List<String>> rows;
    private final long rowCount;

    public static ProblemResultPreviewRes from(ProblemOutputPreviewOutput output) {
        return new ProblemResultPreviewRes(output.getColumns(), output.getRows(), output.getRowCount());
    }
}
