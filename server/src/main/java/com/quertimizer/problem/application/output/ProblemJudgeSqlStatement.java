package com.quertimizer.problem.application.output;

import com.quertimizer.problem.domain.model.ProblemJudgeExecutionMode;
import lombok.Data;

@Data
public class ProblemJudgeSqlStatement {

    private final String sql;
    private final ProblemJudgeExecutionMode mode;
}
