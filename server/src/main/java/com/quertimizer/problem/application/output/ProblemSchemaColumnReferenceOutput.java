package com.quertimizer.problem.application.output;

import lombok.Data;

@Data
public class ProblemSchemaColumnReferenceOutput {

    private final String tableName;
    private final String columnName;
}
