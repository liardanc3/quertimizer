package com.quertimizer.problem.application.output;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ProblemSubmissionProgress {

    private final String problemId;
    private final String stepKey;
    private final String status;
    private final String message;
    private final List<String> detailLines;
    private final String statementKey;
    private final Integer statementIndex;
    private final String statementSql;
    private final String statementMode;
    private final Boolean statementReference;
}
