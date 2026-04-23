package com.quertimizer.problem.presentation.realtime.dto;

import java.util.List;

public record ProblemSubmitProgressRes(String type,
                                       String problemId,
                                       String stepKey,
                                       String status,
                                       String message,
                                       List<String> detailLines,
                                       String statementKey,
                                       Integer statementIndex,
                                       String statementSql,
                                       String statementMode,
                                       Boolean statementReference) {

    public static ProblemSubmitProgressRes of(String problemId,
                                              String stepKey,
                                              String status,
                                              String message,
                                              List<String> detailLines,
                                              String statementKey,
                                              Integer statementIndex,
                                              String statementSql,
                                              String statementMode,
                                              Boolean statementReference) {
        return new ProblemSubmitProgressRes(
                "problem.submit.progress",
                problemId,
                stepKey,
                status,
                message,
                detailLines != null ? detailLines : List.of(),
                statementKey,
                statementIndex,
                statementSql,
                statementMode,
                statementReference
        );
    }
}
