package com.quertimizer.problem.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.problem.application.port.in.CancelProblemExecutionUseCase;
import com.quertimizer.problem.application.port.out.ProblemJudgePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CancelProblemExecution implements CancelProblemExecutionUseCase {

    private final ProblemJudgePort problemJudgePort;
    private final ProblemExecutionSessionStore executionSessionStore;

    /**
     * 문제 실행 세션의 진행 중 실행을 취소한다.
     *
     * <ol>
     *   <li>실행 세션 조회
     *   <li>judge 실행 취소
     * </ol>
     *
     * @param executionSessionId 취소할 문제 실행 세션 ID
     */
    @Override
    @Log("문제 실행 취소")
    public void execute(String executionSessionId) {
        executionSessionStore.find(executionSessionId)
                .filter(session -> session.getLastExecutionId() != null)
                .ifPresentOrElse(
                        session -> {
                            log.info(
                                    "SQL 실행 취소 요청 executionSessionId={}, executionId={}",
                                    executionSessionId, session.getLastExecutionId()
                            );
                            problemJudgePort.cancelExecution(session.getLastExecutionId());
                        },
                        () -> log.info("SQL 실행 취소 대상 없음 executionSessionId={}", executionSessionId)
                );
    }
}
