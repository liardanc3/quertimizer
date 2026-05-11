package com.quertimizer.problem.application.output;

import lombok.Data;

@Data
public class ProblemExamplePreviewOutput {

    private final ProblemDataExampleOutput dataExample;
    private final ProblemOutputPreviewOutput outputExample;
}
