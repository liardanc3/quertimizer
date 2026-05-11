package com.quertimizer.problem.application.service;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.port.out.ProblemSolveHistoryRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemSubmitHistoryRepositoryPort;
import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import com.quertimizer.problem.domain.entity.ids.ProblemSolveHistoryId;
import com.quertimizer.problem.domain.entity.ProblemSubmitHistory;
import com.quertimizer.problem.domain.policy.ProblemSolveHistoryPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProblemSubmissionRecordService {

    private final ProblemSubmitHistoryRepositoryPort problemSubmitHistoryRepository;
    private final ProblemSolveHistoryRepositoryPort problemSolveHistoryRepository;
    private final ProblemSolveHistoryPolicy problemSolveHistoryPolicy;

    public void saveSubmission(String problemId, String handle, DbmsType dbmsType, String submittedSql,
                               boolean success, String message, long executionTimeMs, Double cost,
                               long rowCount, long executionPlanElement, LocalDateTime submittedAt) {
        // 성공 제출만 Cost와 실행계획 요소 저장
        double storedCost = success && cost != null ? cost : 0d;
        long storedExecutionPlanElement = success ? executionPlanElement : 0L;

        // 제출 내역은 성공과 실패 모두 동일한 형식으로 누적 저장
        problemSubmitHistoryRepository.save(ProblemSubmitHistory.create(
                problemId,
                handle,
                dbmsType,
                submittedSql,
                success,
                message,
                executionTimeMs,
                storedCost,
                rowCount,
                storedExecutionPlanElement,
                submittedAt
        ));
    }

    public void saveBestSolveHistory(String problemId, String handle, DbmsType dbmsType, String submittedSql,
                                     long executionTimeMs, Double cost, long scanRows,
                                     long executionPlanElement, LocalDateTime submittedAt) {
        // 비용이 없으면 랭킹 비교 기준을 만들 수 없으므로 최고 기록 갱신 생략
        if (cost == null) {
            return;
        }

        // 현재 최고 기록과 비교해 더 좋은 제출만 저장
        Optional<ProblemSolveHistory> currentTopHistory =
                problemSolveHistoryRepository.findById(new ProblemSolveHistoryId(problemId, handle));
        if (currentTopHistory.isPresent()
                && !problemSolveHistoryPolicy.isBetterThanCurrent(currentTopHistory.get(), cost, executionTimeMs, submittedAt)) {
            return;
        }

        // 최고 기록 저장
        problemSolveHistoryRepository.save(ProblemSolveHistory.create(
                problemId,
                handle,
                dbmsType,
                submittedSql,
                executionTimeMs,
                cost,
                scanRows,
                executionPlanElement,
                submittedAt
        ));
    }
}
