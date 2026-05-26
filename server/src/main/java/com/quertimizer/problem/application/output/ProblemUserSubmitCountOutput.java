package com.quertimizer.problem.application.output;

import lombok.Data;

@Data
public class ProblemUserSubmitCountOutput {

    private final String handle;
    private final long submitCount;
}
