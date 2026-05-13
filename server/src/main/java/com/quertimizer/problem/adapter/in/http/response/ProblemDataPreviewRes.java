package com.quertimizer.problem.adapter.in.http.response;

import com.quertimizer.problem.application.output.ProblemDataExampleOutput;
import com.quertimizer.problem.application.output.ProblemExampleTableOutput;
import lombok.Data;

import java.util.List;

@Data
public class ProblemDataPreviewRes {

    private final int rowLimit;
    private final List<ProblemExampleTableOutput> tables;

    public static ProblemDataPreviewRes from(ProblemDataExampleOutput output) {
        return new ProblemDataPreviewRes(output.getRowLimit(), output.getTables());
    }
}
