package com.quertimizer.problem.application.usecase;

import com.quertimizer.problem.application.port.ProblemExecutionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 문제 SQL 실행 세션을 종료한다.
 */
@Component
@RequiredArgsConstructor
public class CloseProblemExecutionSession {

    private final ProblemExecutionPort problemExecutionPort;

    /**
     * 문제 실행 세션을 종료한다.
     *
     * @param executionSessionId 종료할 문제 실행 세션 ID
     */
    public void execute(String executionSessionId) {
        problemExecutionPort.closeSession(executionSessionId);
    }
}
