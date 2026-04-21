package com.quertimizer.endpoint.api.dto.response;

import com.quertimizer.constant.DbmsType;
import com.quertimizer.entity.Problem;
import com.quertimizer.entity.ProblemSet;
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
    private final String answer;
    private final String answerHash;
    private final String dbms;

    public static ProblemDetailRes from(Problem problem, ProblemSet problemSet) {
        return new ProblemDetailRes(
                problem.getProblemId(),
                problem.getTitle(),
                normalize(problem.getDescription()),
                problem.getDbmsType() == DbmsType.POSTGRESQL ? normalize(problem.getDdl()) : "",
                problem.getDbmsType() == DbmsType.ORACLE ? normalize(problem.getDdl()) : "",
                problemSet.getDbmsType() == DbmsType.POSTGRESQL ? normalize(problemSet.getData()) : "",
                problemSet.getDbmsType() == DbmsType.ORACLE ? normalize(problemSet.getData()) : "",
                normalize(problem.getCondition()),
                normalize(problem.getOutput()),
                normalize(problem.getOutputSample()),
                normalizeAnswerSql(problem),
                normalize(problem.getAnswer()),
                problem.getDbmsType().getValue()
        );
    }

    public static ProblemDetailRes from(Problem problem) {
        return new ProblemDetailRes(
                problem.getProblemId(),
                problem.getTitle(),
                normalize(problem.getDescription()),
                problem.getDbmsType() == DbmsType.POSTGRESQL ? normalize(problem.getDdl()) : "",
                problem.getDbmsType() == DbmsType.ORACLE ? normalize(problem.getDdl()) : "",
                "",
                "",
                normalize(problem.getCondition()),
                normalize(problem.getOutput()),
                normalize(problem.getOutputSample()),
                normalizeAnswerSql(problem),
                normalize(problem.getAnswer()),
                problem.getDbmsType().getValue()
        );
    }

    private static String normalizeAnswerSql(Problem problem) {
        if (problem.getAnswerSql() != null && !problem.getAnswerSql().isBlank()) {
            return problem.getAnswerSql();
        }

        return normalize(problem.getAnswer());
    }

    private static String normalize(String value) {
        return value != null ? value : "";
    }

}
