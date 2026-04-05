package com.quertimizer.endpoint.api.dto.response;

import com.quertimizer.entity.Problem;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProblemDetailRes {

    private final String problemId;
    private final String title;
    private final String description;
    private final String ddl;
    private final String condition;
    private final String output;
    private final String dataSample;
    private final String outputSample;

    public static ProblemDetailRes from(Problem problem) {
        return new ProblemDetailRes(
                problem.getProblemId(),
                problem.getTitle(),
                normalize(problem.getDescription()),
                normalize(problem.getDdl()),
                normalize(problem.getCondition()),
                normalize(problem.getOutput()),
                normalize(problem.getDataSample()),
                normalize(problem.getOutputSample())
        );
    }

    private static String normalize(String value) {
        return value != null ? value : "";
    }

}
