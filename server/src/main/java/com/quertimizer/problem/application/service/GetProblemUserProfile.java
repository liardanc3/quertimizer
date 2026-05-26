package com.quertimizer.problem.application.service;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.output.ProblemUserProfileOutput;
import com.quertimizer.problem.application.output.ProblemUserSolvedRecordOutput;
import com.quertimizer.problem.application.output.ProblemUserSubmissionActivityOutput;
import com.quertimizer.problem.application.port.in.GetProblemUserProfileUseCase;
import com.quertimizer.problem.application.port.out.ProblemRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemSolveHistoryRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemSubmitHistoryRepositoryPort;
import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import com.quertimizer.problem.domain.entity.ProblemSubmitHistory;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetProblemUserProfile implements GetProblemUserProfileUseCase {

    private final ProblemSolveHistoryRepositoryPort problemSolveHistoryRepository;
    private final ProblemSubmitHistoryRepositoryPort problemSubmitHistoryRepository;
    private final ProblemRepositoryPort problemRepository;

    /**
     * 사용자 프로필에 필요한 문제 풀이 기록을 조회한다.
     *
     * <ol>
     *   <li>사용자 최고 해결 기록 계산
     *   <li>사용자 제출 활동 집계
     *   <li>프로필 전용 응답 생성
     * </ol>
     *
     * @param handle 프로필 대상 사용자 handle
     */
    @Override
    @Transactional(readOnly = true)
    public ProblemUserProfileOutput execute(String handle) {
        List<ProblemSolveHistory> bestSolvedHistories = createBestSolvedHistories(
                problemSolveHistoryRepository.findAllByHandleOrderBySubmittedAtDesc(handle)
        );

        List<ProblemSubmitHistory> submitHistories = problemSubmitHistoryRepository.findAllByHandleOrderBySubmittedAtDesc(handle);

        return new ProblemUserProfileOutput(
                bestSolvedHistories.size(),
                bestSolvedHistories.stream().mapToLong(ProblemSolveHistory::getExecutionTimeMs).sum(),
                calculateAverageExecutionPercentile(bestSolvedHistories, DbmsType.POSTGRESQL),
                calculateAverageExecutionPercentile(bestSolvedHistories, DbmsType.MYSQL),
                createSolvedProblemIds(bestSolvedHistories),
                bestSolvedHistories.stream().map(this::toSolvedRecordOutput).toList(),
                createAttemptedProblemIds(submitHistories),
                createSubmissionActivityOutputs(submitHistories)
        );
    }

    private List<ProblemSolveHistory> createBestSolvedHistories(List<ProblemSolveHistory> histories) {
        // 문제, DBMS별 최고 기록만 추출
        Map<UserSolvedHistoryKey, ProblemSolveHistory> bestHistoryByKey = new HashMap<>();
        for (ProblemSolveHistory history : histories) {
            bestHistoryByKey.merge(
                    new UserSolvedHistoryKey(history.getProblemId(), resolveDbmsType(history)),
                    history,
                    this::pickBetterHistory
            );
        }

        // 최신 순으로 프로필 기록 정렬
        return bestHistoryByKey.values().stream()
                .sorted(Comparator.comparing(ProblemSolveHistory::getSubmittedAt).reversed())
                .toList();
    }

    private List<String> createSolvedProblemIds(List<ProblemSolveHistory> histories) {
        // 해결한 문제 번호 목록 생성
        return histories.stream()
                .map(ProblemSolveHistory::getProblemId)
                .distinct()
                .sorted()
                .toList();
    }

    private List<String> createAttemptedProblemIds(List<ProblemSubmitHistory> histories) {
        // 제출한 문제 번호 목록 생성
        return histories.stream()
                .map(ProblemSubmitHistory::getProblemId)
                .distinct()
                .sorted()
                .toList();
    }

    private List<ProblemUserSubmissionActivityOutput> createSubmissionActivityOutputs(List<ProblemSubmitHistory> histories) {
        // 제출 일자별 횟수 목록 생성
        Map<String, Long> countByDate = new LinkedHashMap<>();
        histories.forEach(history -> {
            String submittedDate = history.getSubmittedAt().toLocalDate().toString();
            countByDate.put(submittedDate, countByDate.getOrDefault(submittedDate, 0L) + 1);
        });

        return countByDate.entrySet().stream()
                .map(entry -> new ProblemUserSubmissionActivityOutput(entry.getKey(), entry.getValue()))
                .toList();
    }

    private ProblemUserSolvedRecordOutput toSolvedRecordOutput(ProblemSolveHistory history) {
        // 풀이 기록 응답 생성
        return new ProblemUserSolvedRecordOutput(
                history.getProblemId(),
                problemRepository.findByProblemId(history.getProblemId())
                        .map(problem -> problem.getTitle())
                        .orElse(history.getProblemId()),
                resolveDbmsType(history).getValue(),
                history.getExecutionTimeMs(),
                history.getCost(),
                history.getSubmittedAt()
        );
    }

    private Double calculateAverageExecutionPercentile(List<ProblemSolveHistory> bestSolvedHistories, DbmsType dbmsType) {
        // 평균 실행 백분위 계산
        List<Integer> executionPercentiles = bestSolvedHistories.stream()
                .filter(history -> resolveDbmsType(history) == dbmsType)
                .map(this::calculateExecutionPercentile)
                .flatMap(Optional::stream)
                .toList();

        if (executionPercentiles.isEmpty()) {
            return null;
        }

        double averageExecutionPercentile = executionPercentiles.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0);
        return Math.round(averageExecutionPercentile * 10d) / 10d;
    }

    private Optional<Integer> calculateExecutionPercentile(ProblemSolveHistory history) {
        // 실행 백분위 계산 대상 조회
        List<ProblemSolveHistory> bestSubmittedHistories = problemSolveHistoryRepository.findAllByProblemId(history.getProblemId()).stream()
                .filter(candidateHistory -> resolveDbmsType(candidateHistory) == resolveDbmsType(history))
                .sorted(
                        Comparator.comparingDouble(ProblemSolveHistory::getCost)
                                .thenComparingLong(ProblemSolveHistory::getExecutionTimeMs)
                                .thenComparing(ProblemSolveHistory::getHandle)
                )
                .toList();

        // 비교 기록 없으면 백분위 생략
        if (bestSubmittedHistories.isEmpty()) {
            return Optional.empty();
        }

        // 더 빠른 기록 수 기준 백분위 반환
        long fasterHistoryCount = bestSubmittedHistories.stream()
                .filter(candidateHistory -> candidateHistory.getExecutionTimeMs() < history.getExecutionTimeMs())
                .count();
        return Optional.of(Math.max(
                1,
                (int) Math.round(((fasterHistoryCount + 1d) / (bestSubmittedHistories.size() + 1d)) * 100d)
        ));
    }

    private ProblemSolveHistory pickBetterHistory(ProblemSolveHistory currentHistory, ProblemSolveHistory candidateHistory) {
        // Cost가 더 낮은 기록 선택
        if (candidateHistory.getCost() < currentHistory.getCost()) {
            return candidateHistory;
        }

        if (candidateHistory.getCost() > currentHistory.getCost()) {
            return currentHistory;
        }

        // Cost가 같으면 실행 시간이 더 짧은 기록 선택
        if (candidateHistory.getExecutionTimeMs() < currentHistory.getExecutionTimeMs()) {
            return candidateHistory;
        }

        if (candidateHistory.getExecutionTimeMs() > currentHistory.getExecutionTimeMs()) {
            return currentHistory;
        }

        // Cost와 실행 시간이 같으면 더 먼저 제출한 기록 선택
        if (candidateHistory.getSubmittedAt().isBefore(currentHistory.getSubmittedAt())) {
            return candidateHistory;
        }

        return currentHistory;
    }

    private DbmsType resolveDbmsType(ProblemSolveHistory history) {
        // 오래된 해결 이력은 PostgreSQL 기록으로 간주
        return history.getDbmsType() != null ? history.getDbmsType() : DbmsType.POSTGRESQL;
    }

    @Value
    @Accessors(fluent = true)
    private static class UserSolvedHistoryKey {
        String problemId;
        DbmsType dbmsType;
    }
}
