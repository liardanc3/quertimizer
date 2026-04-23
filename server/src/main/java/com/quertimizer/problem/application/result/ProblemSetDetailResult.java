package com.quertimizer.problem.application.result;

public record ProblemSetDetailResult(String problemSetId,
                                     String ddlPostgresql,
                                     String ddlOracle,
                                     String dataPostgresql,
                                     String dataOracle) {
}
