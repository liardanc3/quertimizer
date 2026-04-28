package com.quertimizer.problem.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ProblemSetAccessInput {

    private final String problemSetId;
    private final String authenticatedEmail;
}
