package com.quertimizer.problem.presentation.dto.response;

import com.quertimizer.problem.application.result.ProblemSubmittedHistoryResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProblemSubmittedHistoryRes {

    private final String dbms;
    private final String handle;
    private final long executionPlanElement;
    private final long executionTimeMs;
    private final double cost;

    public static ProblemSubmittedHistoryRes from(ProblemSubmittedHistoryResult result) {
        return new ProblemSubmittedHistoryRes(
                result.dbms(),
                result.handle(),
                result.executionPlanElement(),
                result.executionTimeMs(),
                result.cost()
        );
    }
}
