package com.quertimizer.problem.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateProblemInput {

    private final ProblemCreateInput problem;
    private final String authenticatedEmail;
}
