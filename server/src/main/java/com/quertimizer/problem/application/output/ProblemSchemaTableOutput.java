package com.quertimizer.problem.application.output;

import lombok.Data;

import java.util.List;

@Data
public class ProblemSchemaTableOutput {

    private final String name;
    private final String description;
    private final List<ProblemSchemaColumnOutput> columns;
}
