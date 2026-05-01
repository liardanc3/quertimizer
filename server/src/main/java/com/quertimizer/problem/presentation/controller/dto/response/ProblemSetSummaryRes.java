package com.quertimizer.problem.presentation.controller.dto.response;

import com.quertimizer.problem.application.output.ProblemSetSummaryOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProblemSetSummaryRes {

    private final String problemSetId;

    public static ProblemSetSummaryRes from(ProblemSetSummaryOutput result) {
        return new ProblemSetSummaryRes(result.problemSetId());
    }
}
