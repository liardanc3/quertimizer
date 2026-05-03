package com.quertimizer.problem.domain.policy;

import com.quertimizer.problem.domain.entity.ProblemSolveHistory;

import java.time.LocalDateTime;

public class ProblemSolveHistoryPolicy {
    public boolean isBetterThanCurrent(ProblemSolveHistory currentHistory,
                                       double candidateCost,
                                       long candidateExecutionTimeMs,
                                       LocalDateTime candidateSubmittedAt) {
        // 비용, 실행 시간, 제출 시각 순서로 최고 기록 우선순위 판단
        if (candidateCost < currentHistory.getCost()) {
            return true;
        }

        if (candidateCost > currentHistory.getCost()) {
            return false;
        }

        if (candidateExecutionTimeMs < currentHistory.getExecutionTimeMs()) {
            return true;
        }

        if (candidateExecutionTimeMs > currentHistory.getExecutionTimeMs()) {
            return false;
        }

        return candidateSubmittedAt.isBefore(currentHistory.getSubmittedAt());
    }
}
