package com.quertimizer.problem.application.output;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class ProblemSetDetailOutput {

    private final String problemSetId;
    private final String ddl;
    private final String actualDataSql;
}
