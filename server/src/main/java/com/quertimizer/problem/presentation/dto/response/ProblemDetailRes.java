package com.quertimizer.problem.presentation.dto.response;

import com.quertimizer.problem.application.output.ProblemDetailOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProblemDetailRes {

    private final String problemId;
    private final String title;
    private final String description;
    private final String ddlPostgresql;
    private final String ddlOracle;
    private final String dataPostgresql;
    private final String dataOracle;
    private final String condition;
    private final String output;
    private final String outputSample;
    private final String sampleDataSql;
    private final String answerSql;
    private final String answerHash;
    private final String dbms;

    public static ProblemDetailRes from(ProblemDetailOutput result) {
        return new ProblemDetailRes(
                result.problemId(),
                result.title(),
                result.description(),
                result.ddlPostgresql(),
                result.ddlOracle(),
                result.dataPostgresql(),
                result.dataOracle(),
                result.condition(),
                result.output(),
                result.outputSample(),
                result.sampleDataSql(),
                result.answerSql(),
                result.answerHash(),
                result.dbms()
        );
    }

}
