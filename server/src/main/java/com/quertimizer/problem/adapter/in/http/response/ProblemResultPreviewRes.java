package com.quertimizer.problem.adapter.in.http.response;

import com.quertimizer.problem.application.output.ProblemOutputPreviewOutput;
import lombok.Data;

import java.util.List;

@Data
public class ProblemResultPreviewRes {

    private final List<String> columns;
    private final List<List<String>> rows;
    private final long rowCount;
    private final int visibleRows;
    private final int rowLimit;

    public static ProblemResultPreviewRes from(ProblemOutputPreviewOutput output) {
        return new ProblemResultPreviewRes(
                output.getColumns(), output.getRows(),
                output.getRowCount(), output.getVisibleRows(), output.getRowLimit()
        );
    }
}
