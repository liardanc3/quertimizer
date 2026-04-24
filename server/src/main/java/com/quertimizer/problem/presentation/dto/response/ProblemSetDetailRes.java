package com.quertimizer.problem.presentation.dto.response;

import com.quertimizer.problem.application.output.ProblemSetDetailOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProblemSetDetailRes {

    private final String problemSetId;
    private final String ddlPostgresql;
    private final String ddlOracle;
    private final String dataPostgresql;
    private final String dataOracle;

    public static ProblemSetDetailRes from(ProblemSetDetailOutput result) {
        return new ProblemSetDetailRes(
                result.problemSetId(),
                result.ddlPostgresql(),
                result.ddlOracle(),
                result.dataPostgresql(),
                result.dataOracle()
        );
    }
}
