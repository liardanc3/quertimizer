package com.quertimizer.problem.application.output;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ProblemSubmissionOutput {

    private final String problemId;
    private final boolean success;
    private final String message;
    private final Long executionTimeMs;
}
