package com.quertimizer.problem.domain.policy;

import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 문제 최고 기록 갱신 여부를 판단한다.
 */
@Component
public class ProblemSolveHistoryPolicy {

    /**
     * 제출 결과가 현재 최고 기록보다 좋은 기록인지 확인한다.
     *
     * @param currentHistory 현재 저장된 최고 기록
     * @param candidateCost 제출 결과의 실행 비용
     * @param candidateExecutionTimeMs 제출 결과의 실행 시간
     * @param candidateSubmittedAt 제출 시각
     * @return 최고 기록 갱신 가능 여부
     */
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
