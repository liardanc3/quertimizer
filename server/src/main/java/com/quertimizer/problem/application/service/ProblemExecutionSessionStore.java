package com.quertimizer.problem.application.service;

import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ProblemExecutionSessionStore {

    private final ConcurrentHashMap<String, ProblemExecutionSession> sessionById = new ConcurrentHashMap<>();
    public Optional<ProblemExecutionSession> find(String executionSessionId) {
        // 실행 세션 ID 기준 상태 조회
        return Optional.ofNullable(sessionById.get(executionSessionId));
    }

    public Optional<ProblemExecutionSession> findReusable(String executionSessionId, String problemId, String datasetId) {
        // 같은 문제와 데이터셋을 바라보는 재사용 가능 세션 조회
        return find(executionSessionId)
                .filter(session -> session.matches(problemId, datasetId));
    }

    public ProblemExecutionSession save(String executionSessionId, String problemId, String datasetId, String environmentId) {
        // 실행 세션 상태 저장
        ProblemExecutionSession session = new ProblemExecutionSession(problemId, datasetId, environmentId);
        sessionById.put(executionSessionId, session);
        return session;
    }

    public Optional<ProblemExecutionSession> remove(String executionSessionId) {
        // 실행 세션 상태 제거 후 반환
        return Optional.ofNullable(sessionById.remove(executionSessionId));
    }

    public void markExecution(String executionSessionId, String executionId) {
        // 실행 취소 대상이 되는 마지막 실행 ID 보관
        find(executionSessionId).ifPresent(session -> session.lastExecutionId = executionId);
    }

    public void clearExecution(String executionSessionId, String executionId) {
        // 완료된 실행이 마지막 실행이면 취소 대상에서 제거
        find(executionSessionId).ifPresent(session -> session.clearExecution(executionId));
    }

    public static final class ProblemExecutionSession {
        private final String problemId;
        private final String datasetId;
        private final String environmentId;
        private String lastExecutionId;

        private ProblemExecutionSession(String problemId, String datasetId, String environmentId) {
            this.problemId = problemId;
            this.datasetId = datasetId;
            this.environmentId = environmentId;
        }

    public String getEnvironmentId() {
            // judge 실행 환경 ID 반환
            return environmentId;
        }

    public String getLastExecutionId() {
            // 마지막 judge 실행 ID 반환
            return lastExecutionId;
        }

        private boolean matches(String problemId, String datasetId) {
            // 문제와 데이터셋 일치 여부 확인
            return this.problemId.equals(problemId) && this.datasetId.equals(datasetId);
        }

        private void clearExecution(String executionId) {
            // 완료된 실행 ID 기준 마지막 실행 ID 정리
            if (executionId.equals(lastExecutionId)) {
                lastExecutionId = null;
            }
        }
    }
}
