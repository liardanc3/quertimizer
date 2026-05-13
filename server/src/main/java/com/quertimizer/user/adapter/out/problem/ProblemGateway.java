package com.quertimizer.user.adapter.out.problem;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.port.out.ProblemRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemSolveHistoryRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemSubmitHistoryRepositoryPort;
import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import com.quertimizer.problem.domain.entity.ProblemSubmitHistory;
import com.quertimizer.user.application.output.UserProfileSolvedProblemsOutput;
import com.quertimizer.user.application.output.UserProfileSolvedRecordOutput;
import com.quertimizer.user.application.output.UserProfileSolvedRecordsOutput;
import com.quertimizer.user.application.output.UserProfileSubmissionActivityOutput;
import com.quertimizer.user.application.output.UserProfileSubmissionSummaryOutput;
import com.quertimizer.user.application.port.out.UserAnomalySubmitTrendPort;
import com.quertimizer.user.application.port.out.UserProfileProblemPort;
import com.quertimizer.user.domain.model.UserAnomalySubmitTrend;
import com.quertimizer.user.domain.model.UserProfileProblemSummary;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component("userProblemGateway")
@RequiredArgsConstructor
public class ProblemGateway implements UserProfileProblemPort, UserAnomalySubmitTrendPort {

    private final ProblemSolveHistoryRepositoryPort problemSolveHistoryRepository;
    private final ProblemSubmitHistoryRepositoryPort problemSubmitHistoryRepository;
    private final ProblemRepositoryPort problemRepository;

    @Override
    public UserProfileProblemSummary getProblemSummary(String handle) {
        // 사용자 해결 기록 기준 최고 기록 계산
        List<ProblemSolveHistory> histories = problemSolveHistoryRepository.findAllByHandleOrderBySubmittedAtDesc(handle);
        List<ProblemSolveHistory> bestSolvedHistories = createBestSolvedHistories(histories);

        // 해결 수와 실행 시간 합계 및 DBMS별 평균 실행 백분위 반환
        return new UserProfileProblemSummary(
                bestSolvedHistories.size(),
                bestSolvedHistories.stream().mapToLong(ProblemSolveHistory::getExecutionTimeMs).sum(),
                calculateAverageExecutionPercentile(bestSolvedHistories, DbmsType.POSTGRESQL),
                calculateAverageExecutionPercentile(bestSolvedHistories, DbmsType.MYSQL)
        );
    }

    @Override
    public UserProfileSolvedProblemsOutput getSolvedProblems(String handle) {
        // 사용자 최고 해결 기록 기준 문제 번호 목록 반환
        List<String> solvedProblemIds = createBestSolvedHistories(
                problemSolveHistoryRepository.findAllByHandleOrderBySubmittedAtDesc(handle)
        ).stream()
                .map(ProblemSolveHistory::getProblemId)
                .distinct()
                .sorted()
                .toList();
        return new UserProfileSolvedProblemsOutput(solvedProblemIds.size(), solvedProblemIds);
    }

    @Override
    public UserProfileSolvedRecordsOutput getSolvedRecords(String handle) {
        // 사용자 최고 해결 기록 목록을 프로필 응답으로 변환
        return new UserProfileSolvedRecordsOutput(createBestSolvedHistories(
                problemSolveHistoryRepository.findAllByHandleOrderBySubmittedAtDesc(handle)
        ).stream()
                .map(this::toSolvedRecordOutput)
                .toList());
    }

    @Override
    public UserProfileSubmissionSummaryOutput getSubmissionSummary(String handle) {
        // 사용자 제출 기록 기준 제출 문제와 일자별 활동 반환
        List<ProblemSubmitHistory> submitHistories = problemSubmitHistoryRepository.findAllByHandleOrderBySubmittedAtDesc(handle);
        return new UserProfileSubmissionSummaryOutput(
                createAttemptedProblemIds(submitHistories),
                createSubmissionActivityOutputs(submitHistories)
        );
    }

    @Override
    public Page<UserAnomalySubmitTrend> findUserSubmitCounts(Pageable pageable) {
        // problem 제출 집계 조회 후 user 이상 제출 추세 모델 변환
        return problemSubmitHistoryRepository.findUserSubmitCounts(pageable).map(this::toSubmitTrend);
    }

    @Override
    public Page<UserAnomalySubmitTrend> findUserSubmitCountsSince(LocalDateTime submittedAfter, Pageable pageable) {
        // 기준 시간 이후 problem 제출 집계 조회 후 user 이상 제출 추세 모델 변환
        return problemSubmitHistoryRepository.findUserSubmitCountsSince(submittedAfter, pageable).map(this::toSubmitTrend);
    }

    @Override
    public Page<UserAnomalySubmitTrend> findUserSubmitCountsBetween(LocalDateTime submittedStart,
                                                                    LocalDateTime submittedEnd,
                                                                    Pageable pageable) {
        // 기준 시간 범위 problem 제출 집계 조회 후 user 이상 제출 추세 모델 변환
        return problemSubmitHistoryRepository.findUserSubmitCountsBetween(submittedStart, submittedEnd, pageable)
                .map(this::toSubmitTrend);
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

    private List<String> createAttemptedProblemIds(List<ProblemSubmitHistory> histories) {
        // 제출한 문제 번호 목록 생성
        return histories.stream()
                .map(ProblemSubmitHistory::getProblemId)
                .distinct()
                .sorted()
                .toList();
    }

    private List<UserProfileSubmissionActivityOutput> createSubmissionActivityOutputs(List<ProblemSubmitHistory> histories) {
        // 제출 일자별 횟수 목록 생성
        Map<String, Long> countByDate = new LinkedHashMap<>();
        histories.forEach(history -> {
            String submittedDate = history.getSubmittedAt().toLocalDate().toString();
            countByDate.put(submittedDate, countByDate.getOrDefault(submittedDate, 0L) + 1);
        });

        return countByDate.entrySet().stream()
                .map(entry -> new UserProfileSubmissionActivityOutput(entry.getKey(), entry.getValue()))
                .toList();
    }

    private UserProfileSolvedRecordOutput toSolvedRecordOutput(ProblemSolveHistory history) {
        // 풀이 기록 응답 생성
        return new UserProfileSolvedRecordOutput(
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

    private UserAnomalySubmitTrend toSubmitTrend(ProblemSubmitHistoryRepositoryPort.UserSubmitCountProjection projection) {
        // problem 제출 집계 projection을 user 이상 제출 추세 모델로 변환
        return new UserAnomalySubmitTrend(projection.getHandle(), projection.getSubmitCount());
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
        // 사용자 해결 기록 키 보관
        String problemId;
        DbmsType dbmsType;
    }

}
