package com.quertimizer.problem.application.output;

import lombok.Data;

@Data
public class ProblemSchemaRelationOutput {

    private final String sourceTableName;
    private final String sourceColumnName;
    private final String targetTableName;
    private final String targetColumnName;
}
