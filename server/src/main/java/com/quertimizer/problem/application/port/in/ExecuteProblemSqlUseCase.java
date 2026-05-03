package com.quertimizer.problem.application.port.in;

import com.quertimizer.problem.application.input.ProblemExecutionInput;
import com.quertimizer.problem.application.output.ProblemExecutionOutput;

public interface ExecuteProblemSqlUseCase {

    ProblemExecutionOutput execute(ProblemExecutionInput input);
}
