package com.quertimizer.problem.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class ProblemDetailOutput {

    private final String problemId;
    private final String title;
    private final String description;
    private final String ddlPostgresql;
    private final String ddlMysql;
    private final String dataPostgresql;
    private final String dataMysql;
    private final String condition;
    private final String output;
    private final String outputSample;
    private final String sampleDataSql;
    private final String answerSql;
    private final String answerHash;
    private final String dbms;
}
