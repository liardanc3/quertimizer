package com.quertimizer.problem.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class ProblemSetDetailOutput {

    private final String problemSetId;
    private final String ddlPostgresql;
    private final String ddlMysql;
    private final String dataPostgresql;
    private final String dataMysql;
}
