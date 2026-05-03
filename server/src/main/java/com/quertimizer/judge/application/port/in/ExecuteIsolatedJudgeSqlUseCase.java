package com.quertimizer.judge.application.port.in;

import com.quertimizer.judge.application.input.ExecuteIsolatedJudgeSqlInput;
import com.quertimizer.judge.application.output.SqlExecutionResult;

public interface ExecuteIsolatedJudgeSqlUseCase {

    SqlExecutionResult execute(ExecuteIsolatedJudgeSqlInput input);
}
