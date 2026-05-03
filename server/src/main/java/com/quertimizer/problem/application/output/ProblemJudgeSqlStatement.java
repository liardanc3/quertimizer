package com.quertimizer.problem.application.output;

import com.quertimizer.problem.domain.model.ProblemJudgeExecutionMode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ProblemJudgeSqlStatement {

    private final String sql;
    private final ProblemJudgeExecutionMode mode;
}
