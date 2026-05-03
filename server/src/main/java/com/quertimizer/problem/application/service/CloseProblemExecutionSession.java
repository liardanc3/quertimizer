package com.quertimizer.problem.application.service;

import com.quertimizer.problem.application.port.in.CloseProblemExecutionSessionUseCase;
import com.quertimizer.problem.application.port.out.ProblemJudgePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CloseProblemExecutionSession implements CloseProblemExecutionSessionUseCase {

    private final ProblemJudgePort problemJudgePort;
    private final ProblemExecutionSessionStore executionSessionStore;

    /**
     * 문제 실행 세션을 종료한다.
     *
     * <ol>
     *   <li>실행 세션 제거
     *   <li>judge 실행 환경 정리
     * </ol>
     *
     * @param executionSessionId 종료할 문제 실행 세션 ID
     */
    @Override
    public void execute(String executionSessionId) {
        executionSessionStore.remove(executionSessionId)
                .ifPresentOrElse(
                        session -> {
                            log.info(
                                    "문제 SQL 실행 세션 정리 시작 executionSessionId={}, environmentId={}",
                                    executionSessionId, session.getEnvironmentId()
                            );
                            dropQuietly(executionSessionId, session.getEnvironmentId());
                        },
                        () -> log.info("문제 SQL 실행 세션 정리 대상 없음 executionSessionId={}", executionSessionId)
                );
    }

    private void dropQuietly(String executionSessionId, String environmentId) {
        // 정리 실패 시 세션 종료 흐름 보호용 로그 기록
        try {
            problemJudgePort.dropEnvironment(environmentId);
            log.info(
                    "문제 SQL 실행 환경 정리 완료 executionSessionId={}, environmentId={}",
                    executionSessionId, environmentId
            );
        } catch (Exception exception) {
            log.warn("judge 실행 환경 정리 실패 executionSessionId={}, environmentId={}", executionSessionId, environmentId, exception);
        }
    }
}
