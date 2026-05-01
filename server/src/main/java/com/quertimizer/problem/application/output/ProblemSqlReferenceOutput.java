package com.quertimizer.problem.application.output;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ProblemSqlReferenceOutput {

    private final String referenceId;
    private final String resultHash;
}
