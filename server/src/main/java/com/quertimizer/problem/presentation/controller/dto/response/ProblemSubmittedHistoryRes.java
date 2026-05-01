package com.quertimizer.problem.presentation.controller.dto.response;

import com.quertimizer.problem.application.output.ProblemSubmittedHistoryOutput;
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

    public static ProblemSubmittedHistoryRes from(ProblemSubmittedHistoryOutput result) {
        return new ProblemSubmittedHistoryRes(
                result.dbms(), result.handle(),
                result.executionPlanElement(), result.executionTimeMs(), result.cost()
        );
    }
}
