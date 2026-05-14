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

    public ProblemExecutionSession save(String executionSessionId, String problemId, Long datasetId, String environmentId) {
        // 실행 세션 상태 저장
        ProblemExecutionSession session = new ProblemExecutionSession(problemId, datasetId, environmentId);
        sessionById.put(executionSessionId, session);
        return session;
    }

    public Optional<ProblemExecutionSession> remove(String executionSessionId) {
        // 실행 세션 상태 제거 후 반환
        return Optional.ofNullable(sessionById.remove(executionSessionId));
    }

    public void remove(String executionSessionId, ProblemExecutionSession session) {
        // 지정한 실행 환경 세션과 일치하는 경우에만 상태 제거
        sessionById.remove(executionSessionId, session);
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
        private final Long datasetId;
        private final String environmentId;
        private String lastExecutionId;

        private ProblemExecutionSession(String problemId, Long datasetId, String environmentId) {
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

        private void clearExecution(String executionId) {
            // 완료된 실행 ID 기준 마지막 실행 ID 정리
            if (executionId.equals(lastExecutionId)) {
                lastExecutionId = null;
            }
        }
    }
}
