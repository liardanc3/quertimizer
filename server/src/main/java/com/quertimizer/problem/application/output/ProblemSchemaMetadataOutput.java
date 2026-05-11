package com.quertimizer.problem.application.output;

import lombok.Data;

import java.util.List;

@Data
public class ProblemSchemaMetadataOutput {

    private final List<ProblemSchemaTableOutput> tables;
    private final List<ProblemSchemaRelationOutput> relations;
}
