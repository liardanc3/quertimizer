package com.quertimizer.problem.application.output;

public record ProblemSetDetailOutput(String problemSetId,
                                     String ddlPostgresql,
                                     String ddlOracle,
                                     String dataPostgresql,
                                     String dataOracle) {
}
