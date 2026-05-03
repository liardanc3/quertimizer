package com.quertimizer.problem.adapter.in.web.response;

import com.quertimizer.problem.application.output.ProblemSetDetailOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProblemSetDetailRes {

    private final String problemSetId;
    private final String ddlPostgresql;
    private final String ddlMysql;
    private final String dataPostgresql;
    private final String dataMysql;

    public static ProblemSetDetailRes from(ProblemSetDetailOutput result) {
        return new ProblemSetDetailRes(
                result.problemSetId(),
                result.ddlPostgresql(), result.ddlMysql(),
                result.dataPostgresql(), result.dataMysql()
        );
    }
}
