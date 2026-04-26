package com.quertimizer.problem.application.output;

public record ProblemDetailOutput(String problemId,
                                  String title,
                                  String description,
                                  String ddlPostgresql,
                                  String ddlOracle,
                                  String dataPostgresql,
                                  String dataOracle,
                                  String condition,
                                  String output,
                                  String outputSample,
                                  String sampleDataSql,
                                  String answerSql,
                                  String answerHash,
                                  String dbms) {
}
