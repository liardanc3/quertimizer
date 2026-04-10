package com.quertimizer.endpoint.api.dto.response;

import com.quertimizer.entity.ProblemSet;
import com.quertimizer.entity.Problem;
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

    public static ProblemDetailRes from(Problem problem, ProblemSet problemSet) {
        return new ProblemDetailRes(
                problem.getProblemId(),
                problem.getTitle(),
                normalize(problem.getDescription()),
                normalize(problem.getDdlPostgresql()),
                normalize(problem.getDdlOracle()),
                normalize(problemSet.getDataPostgresql()),
                normalize(problemSet.getDataOracle()),
                normalize(problem.getCondition()),
                normalize(problem.getOutput()),
                normalize(problem.getOutputSample())
        );
    }

    public static ProblemDetailRes from(Problem problem) {
        return new ProblemDetailRes(
                problem.getProblemId(),
                problem.getTitle(),
                normalize(problem.getDescription()),
                normalize(problem.getDdlPostgresql()),
                normalize(problem.getDdlOracle()),
                "",
                "",
                normalize(problem.getCondition()),
                normalize(problem.getOutput()),
                normalize(problem.getOutputSample())
        );
    }

    private static String normalize(String value) {
        return value != null ? value : "";
    }

}
