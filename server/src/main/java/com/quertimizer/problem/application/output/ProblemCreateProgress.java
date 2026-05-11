package com.quertimizer.problem.application.output;

import lombok.Data;

@Data
public class ProblemCreateProgress {

    private final String stepKey;
    private final String status;
    private final String message;
    private final Integer stepOrder;
}
