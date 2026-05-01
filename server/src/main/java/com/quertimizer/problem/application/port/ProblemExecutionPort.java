package com.quertimizer.problem.application.port;

import com.quertimizer.problem.application.input.ProblemExecutionInput;
import com.quertimizer.problem.application.output.ProblemExecutionOutput;

public interface ProblemExecutionPort {

    ProblemExecutionOutput execute(ProblemExecutionInput input);

    void cancel(String executionSessionId);

    void closeSession(String executionSessionId);
}
