package com.quertimizer.problem.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ProblemCreateInput {

    private final String title;
    private final String description;
    private final String condition;
    private final String output;
    private final String answerSql;
    private final String sampleDataPostgresql;
    private final String sampleDataMysql;
    private final String actualDataPostgresql;
    private final String actualDataMysql;
    private final String problemSetMode;
    private final String problemMode;
    private final String problemSetId;
    private final String problemId;
    private final String dbms;
    private final String ddlPostgresql;
    private final String ddlMysql;
}
