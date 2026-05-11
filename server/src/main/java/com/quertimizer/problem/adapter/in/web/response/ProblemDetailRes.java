package com.quertimizer.problem.adapter.in.web.response;

import com.quertimizer.problem.application.output.ProblemDetailOutput;
import lombok.Data;

@Data
public class ProblemDetailRes {

    private final String problemId;
    private final String title;
    private final String description;
    private final String ddl;
    private final String actualDataSql;
    private final String condition;
    private final String output;
    private final String dataExample;
    private final String outputExample;
    private final String schemaMetadata;
    private final String answerSql;
    private final String answerHash;
    private final String dbms;

    public static ProblemDetailRes from(ProblemDetailOutput result) {
        return new ProblemDetailRes(
                result.problemId(), result.title(), result.description(),
                result.ddl(), result.actualDataSql(), result.condition(),
                result.output(), result.dataExample(), result.outputExample(),
                result.schemaMetadata(), result.answerSql(), result.answerHash(), result.dbms()
        );
    }

}
