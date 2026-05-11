package com.quertimizer.problem.application.output;

import lombok.Data;

import java.util.List;

@Data
public class ProblemDataExampleOutput {

    private final int rowLimit;
    private final List<ProblemExampleTableOutput> tables;
}
