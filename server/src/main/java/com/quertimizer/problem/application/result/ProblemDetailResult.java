package com.quertimizer.problem.application.result;

public record ProblemDetailResult(String problemId,
                                  String title,
                                  String description,
                                  String ddlPostgresql,
                                  String ddlOracle,
                                  String dataPostgresql,
                                  String dataOracle,
                                  String condition,
                                  String output,
                                  String outputSample,
                                  String answer,
                                  String answerHash,
                                  String dbms) {
}
