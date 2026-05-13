package com.quertimizer.problem.adapter.in.http.response;

import com.quertimizer.problem.application.output.ProblemExamplePreviewOutput;
import lombok.Data;

@Data
public class ProblemExamplePreviewRes {

    private final ProblemDataPreviewRes dataExample;
    private final ProblemResultPreviewRes outputExample;

    public static ProblemExamplePreviewRes from(ProblemExamplePreviewOutput output) {
        return new ProblemExamplePreviewRes(
                ProblemDataPreviewRes.from(output.getDataExample()),
                ProblemResultPreviewRes.from(output.getOutputExample())
        );
    }
}
