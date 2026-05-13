package com.quertimizer.problem.adapter.in.http.response;

import com.quertimizer.problem.application.output.ProblemSetDetailOutput;
import lombok.Data;

@Data
public class ProblemSetDetailRes {

    private final String problemSetId;
    private final String ddl;
    private final String actualDataSql;

    public static ProblemSetDetailRes from(ProblemSetDetailOutput result) {
        return new ProblemSetDetailRes(result.problemSetId(), result.ddl(), result.actualDataSql());
    }
}
