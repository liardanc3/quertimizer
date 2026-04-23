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
    private final String outputSample;
    private final String answer;
    private final String answerSql;
    private final String problemSetMode;
    private final String problemMode;
    private final String problemSetId;
    private final String problemId;
    private final String dbms;
    private final String ddlPostgresql;
    private final String ddlOracle;
    private final String dataPostgresql;
    private final String dataOracle;
}
