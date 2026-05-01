package com.quertimizer.problem.application.usecase;

import com.quertimizer.problem.application.port.ProblemExecutionPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 진행 중인 문제 SQL 실행을 취소한다.
 */
@Component
@RequiredArgsConstructor
public class CancelProblemExecution {

    private final ProblemExecutionPort problemExecutionPort;

    /**
     * 문제 실행 세션의 진행 중 실행을 취소한다.
     *
     * @param executionSessionId 취소할 문제 실행 세션 ID
     */
    public void execute(String executionSessionId) {
        problemExecutionPort.cancel(executionSessionId);
    }
}
