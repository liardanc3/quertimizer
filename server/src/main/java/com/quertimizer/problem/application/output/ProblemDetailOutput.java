package com.quertimizer.problem.application.output;

public record ProblemDetailOutput(String problemId,
                                  String title,
                                  String description,
                                  String ddlPostgresql,
                                  String ddlMysql,
                                  String dataPostgresql,
                                  String dataMysql,
                                  String condition,
                                  String output,
                                  String outputSample,
                                  String sampleDataSql,
                                  String answerSql,
                                  String answerHash,
                                  String dbms) {
}
