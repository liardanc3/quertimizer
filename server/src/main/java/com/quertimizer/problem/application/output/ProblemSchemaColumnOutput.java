package com.quertimizer.problem.application.output;

import lombok.Data;

@Data
public class ProblemSchemaColumnOutput {

    private final String name;
    private final String type;
    private final String description;
    private final boolean primaryKey;
    private final boolean foreignKey;
    private final ProblemSchemaColumnReferenceOutput reference;
}
