package com.quertimizer.problem.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class AdminProblemOptionOutput {

    private final String problemId;
}
