package com.quertimizer.judge.application.port.in;

import com.quertimizer.judge.application.input.ExecuteJudgeSqlInput;
import com.quertimizer.judge.application.output.SqlExecutionResult;

public interface ExecuteJudgeSqlUseCase {

    SqlExecutionResult execute(ExecuteJudgeSqlInput input);
}
