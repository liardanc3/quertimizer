package com.quertimizer.problem.application.output;

import com.quertimizer.problem.domain.model.ProblemJudgeExecutionMode;
import lombok.Data;

@Data
public class ProblemSqlStatement {

    private final String sql;
    private final ProblemJudgeExecutionMode mode;
}
