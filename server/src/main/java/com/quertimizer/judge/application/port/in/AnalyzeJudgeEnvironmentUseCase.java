package com.quertimizer.judge.application.port.in;

import com.quertimizer.judge.application.input.AnalyzeJudgeEnvironmentInput;
import com.quertimizer.judge.application.output.SqlExecutionResult;

public interface AnalyzeJudgeEnvironmentUseCase {

    SqlExecutionResult execute(AnalyzeJudgeEnvironmentInput input);
}
