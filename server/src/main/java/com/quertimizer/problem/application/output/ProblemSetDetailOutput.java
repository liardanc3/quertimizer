package com.quertimizer.problem.application.output;

public record ProblemSetDetailOutput(String problemSetId,
                                     String ddlPostgresql,
                                     String ddlMysql,
                                     String dataPostgresql,
                                     String dataMysql) {
}
