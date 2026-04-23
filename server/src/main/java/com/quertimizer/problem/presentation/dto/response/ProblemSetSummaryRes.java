package com.quertimizer.problem.presentation.dto.response;

import com.quertimizer.problem.application.result.ProblemSetSummaryResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProblemSetSummaryRes {

    private final String problemSetId;

    public static ProblemSetSummaryRes from(ProblemSetSummaryResult result) {
        return new ProblemSetSummaryRes(result.problemSetId());
    }
}
