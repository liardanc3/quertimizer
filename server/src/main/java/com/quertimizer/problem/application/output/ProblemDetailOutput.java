package com.quertimizer.problem.application.output;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class ProblemDetailOutput {

    private final String problemId;
    private final String title;
    private final String description;
    private final String ddl;
    private final String actualDataSql;
    private final String condition;
    private final String output;
    private final String dataExample;
    private final String outputExample;
    private final String schemaMetadata;
    private final String answerSql;
    private final String answerHash;
    private final String dbms;
}
