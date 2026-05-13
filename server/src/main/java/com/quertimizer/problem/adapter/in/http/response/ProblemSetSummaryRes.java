package com.quertimizer.problem.adapter.in.http.response;

import com.quertimizer.problem.application.output.ProblemSetSummaryOutput;
import lombok.Data;

@Data
public class ProblemSetSummaryRes {

    private final String problemSetId;

    public static ProblemSetSummaryRes from(ProblemSetSummaryOutput result) {
        return new ProblemSetSummaryRes(result.problemSetId());
    }
}
