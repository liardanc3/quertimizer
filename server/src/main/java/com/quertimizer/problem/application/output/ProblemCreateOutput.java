package com.quertimizer.problem.application.output;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class ProblemCreateOutput {

    private final String problemId;
}
