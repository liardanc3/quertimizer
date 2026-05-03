package com.quertimizer.problem.application.port.in;


public interface CancelProblemExecutionUseCase {

    void execute(String executionSessionId);
}
