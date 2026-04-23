package com.quertimizer.ranking.application.usecase;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import com.quertimizer.problem.infrastructure.repository.ProblemSolveHistoryRepository;
import com.quertimizer.ranking.application.result.RankListItemResult;
import com.quertimizer.ranking.application.result.RankMonthlyDeltaResult;
import com.quertimizer.ranking.application.result.RankPageResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRanks {

    private static final int RANK_PAGE_SIZE = 100;
    private static final RankMonthlyDeltaResult EMPTY_MONTHLY_DELTA = new RankMonthlyDeltaResult(0, 0);

    private final ProblemSolveHistoryRepository problemSolveHistoryRepository;

    public RankPageResult execute(int requestedPage, String dbms, String query, String sortKey) {
        DbmsType dbmsType = resolveDbmsType(dbms);
        RankSortKey rankSortKey = resolveRankSortKey(sortKey);
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        List<ProblemSolveHistory> histories = problemSolveHistoryRepository.findAll();

        // 현재 기준 최고 제출 이력 추출
        List<ProblemSolveHistory> currentBestHistories = createBestHistories(histories, dbmsType, null);

        // 이번달 1일 기준 최고 제출 이력 추출
        List<ProblemSolveHistory> baselineBestHistories = createBestHistories(histories, dbmsType, monthStart);

        // 사용자별 랭킹 지표 계산
        Map<String, RankMetrics> currentMetricsByHandle = createRankMetricsByHandle(currentBestHistories);
        Map<String, RankMetrics> baselineMetricsByHandle = createRankMetricsByHandle(baselineBestHistories);

        // 이번달 1일 대비 순위 변화 계산
        Map<String, RankMonthlyDeltaResult> monthlyDeltaByHandle = createMonthlyDeltaByHandle(
                currentMetricsByHandle,
                baselineMetricsByHandle
        );

        // 검색, 정렬 반영 랭킹 목록 구성
        List<RankListItemResult> filteredRanks = currentMetricsByHandle.values().stream()
                .map(metrics -> new RankListItemResult(
                        metrics.handle(),
                        metrics.solvedCount(),
                        metrics.avgExecutionPercentile(),
                        monthlyDeltaByHandle.getOrDefault(metrics.handle(), EMPTY_MONTHLY_DELTA)
                ))
                .filter(rank -> matchesHandle(rank, query))
                .sorted(createRankComparator(rankSortKey))
                .toList();

        // 페이지 경계 계산
        int totalCount = filteredRanks.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) RANK_PAGE_SIZE));
        int currentPage = Math.min(Math.max(requestedPage, 1), totalPages);
        int fromIndex = Math.min((currentPage - 1) * RANK_PAGE_SIZE, totalCount);
        int toIndex = Math.min(fromIndex + RANK_PAGE_SIZE, totalCount);

        return new RankPageResult(
                currentPage,
                RANK_PAGE_SIZE,
                totalCount,
                totalPages,
                filteredRanks.subList(fromIndex, toIndex)
        );
    }

    private List<ProblemSolveHistory> createBestHistories(List<ProblemSolveHistory> histories,
                                                          DbmsType dbmsType,
                                                          LocalDateTime submittedBefore) {
        Map<UserSolvedHistoryKey, ProblemSolveHistory> bestHistoryByKey = new HashMap<>();

        // 문제, 사용자 기준 최고 제출 추출
        for (ProblemSolveHistory history : histories) {
            if (resolveDbmsType(history) != dbmsType) {
                continue;
            }

            if (submittedBefore != null && !history.getSubmittedAt().isBefore(submittedBefore)) {
                continue;
            }

            UserSolvedHistoryKey historyKey = new UserSolvedHistoryKey(history.getHandle(), history.getProblemId());
            bestHistoryByKey.merge(historyKey, history, this::pickBetterHistory);
        }

        return bestHistoryByKey.values().stream().toList();
    }

    private Map<String, RankMetrics> createRankMetricsByHandle(List<ProblemSolveHistory> bestHistories) {
        Map<String, List<ProblemSolveHistory>> historiesByProblemId = createHistoriesByProblemId(bestHistories);
        Map<UserSolvedHistoryKey, Integer> executionPercentileByHistoryKey =
                createExecutionPercentileByHistoryKey(historiesByProblemId);
        Map<String, List<Integer>> executionPercentilesByHandle = new HashMap<>();
        Map<String, Integer> solvedCountByHandle = new HashMap<>();

        // 사용자별 해결 수, 실행시간 백분위 수집
        for (ProblemSolveHistory history : bestHistories) {
            String handle = history.getHandle();
            solvedCountByHandle.merge(handle, 1, Integer::sum);

            Integer executionPercentile = executionPercentileByHistoryKey.get(
                    new UserSolvedHistoryKey(history.getHandle(), history.getProblemId())
            );

            if (executionPercentile == null) {
                continue;
            }

            executionPercentilesByHandle.computeIfAbsent(handle, key -> new ArrayList<>())
                    .add(executionPercentile);
        }

        Map<String, RankMetrics> metricsByHandle = new HashMap<>();
        for (Map.Entry<String, Integer> solvedCountEntry : solvedCountByHandle.entrySet()) {
            String handle = solvedCountEntry.getKey();
            List<Integer> executionPercentiles = executionPercentilesByHandle.getOrDefault(handle, List.of());

            if (executionPercentiles.isEmpty()) {
                continue;
            }

            double averageExecutionPercentile = executionPercentiles.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0);

            metricsByHandle.put(
                    handle,
                    new RankMetrics(
                            handle,
                            solvedCountEntry.getValue(),
                            Math.round(averageExecutionPercentile * 10d) / 10d
                    )
            );
        }

        return metricsByHandle;
    }

    private Map<String, List<ProblemSolveHistory>> createHistoriesByProblemId(List<ProblemSolveHistory> bestHistories) {
        Map<String, List<ProblemSolveHistory>> historiesByProblemId = new HashMap<>();

        // 문제별 최고 제출 목록 구성
        for (ProblemSolveHistory history : bestHistories) {
            historiesByProblemId.computeIfAbsent(history.getProblemId(), key -> new ArrayList<>())
                    .add(history);
        }

        return historiesByProblemId;
    }

    private Map<UserSolvedHistoryKey, Integer> createExecutionPercentileByHistoryKey(
            Map<String, List<ProblemSolveHistory>> historiesByProblemId
    ) {
        Map<UserSolvedHistoryKey, Integer> executionPercentileByHistoryKey = new HashMap<>();

        // 문제별 실행시간 백분위 계산
        for (List<ProblemSolveHistory> problemHistories : historiesByProblemId.values()) {
            List<ProblemSolveHistory> sortedHistories = problemHistories.stream()
                    .sorted(
                            Comparator.comparingLong(ProblemSolveHistory::getExecutionTimeMs)
                                    .thenComparing(ProblemSolveHistory::getSubmittedAt)
                                    .thenComparing(ProblemSolveHistory::getHandle)
                    )
                    .toList();

            Map<Long, Integer> fasterCountByExecutionTime = new HashMap<>();
            for (int historyIndex = 0; historyIndex < sortedHistories.size(); historyIndex++) {
                fasterCountByExecutionTime.putIfAbsent(
                        sortedHistories.get(historyIndex).getExecutionTimeMs(),
                        historyIndex
                );
            }

            int historyCount = sortedHistories.size();
            for (ProblemSolveHistory history : sortedHistories) {
                int fasterHistoryCount = fasterCountByExecutionTime.getOrDefault(history.getExecutionTimeMs(), 0);
                int executionPercentile = Math.max(
                        1,
                        (int) Math.round(((fasterHistoryCount + 1d) / (historyCount + 1d)) * 100d)
                );

                executionPercentileByHistoryKey.put(
                        new UserSolvedHistoryKey(history.getHandle(), history.getProblemId()),
                        executionPercentile
                );
            }
        }

        return executionPercentileByHistoryKey;
    }

    private Map<String, RankMonthlyDeltaResult> createMonthlyDeltaByHandle(Map<String, RankMetrics> currentMetricsByHandle,
                                                                           Map<String, RankMetrics> baselineMetricsByHandle) {
        Map<String, Integer> currentSolvedCountRankByHandle =
                createRankByHandle(currentMetricsByHandle.values().stream().toList(), RankSortKey.SOLVED_COUNT);
        Map<String, Integer> baselineSolvedCountRankByHandle =
                createRankByHandle(baselineMetricsByHandle.values().stream().toList(), RankSortKey.SOLVED_COUNT);
        Map<String, Integer> currentExecutionPercentileRankByHandle =
                createRankByHandle(currentMetricsByHandle.values().stream().toList(), RankSortKey.AVG_EXECUTION_PERCENTILE);
        Map<String, Integer> baselineExecutionPercentileRankByHandle =
                createRankByHandle(baselineMetricsByHandle.values().stream().toList(), RankSortKey.AVG_EXECUTION_PERCENTILE);

        Map<String, RankMonthlyDeltaResult> monthlyDeltaByHandle = new HashMap<>();
        for (RankMetrics currentMetrics : currentMetricsByHandle.values()) {
            String handle = currentMetrics.handle();

            monthlyDeltaByHandle.put(
                    handle,
                    new RankMonthlyDeltaResult(
                            calculateRankDelta(
                                    currentSolvedCountRankByHandle.get(handle),
                                    baselineSolvedCountRankByHandle.get(handle)
                            ),
                            calculateRankDelta(
                                    currentExecutionPercentileRankByHandle.get(handle),
                                    baselineExecutionPercentileRankByHandle.get(handle)
                            )
                    )
            );
        }

        return monthlyDeltaByHandle;
    }

    private Map<String, Integer> createRankByHandle(List<RankMetrics> metrics, RankSortKey rankSortKey) {
        Map<String, Integer> rankByHandle = new HashMap<>();
        List<RankMetrics> sortedMetrics = metrics.stream()
                .sorted(createRankMetricsComparator(rankSortKey))
                .toList();

        // 정렬 결과 기준 순위 부여
        for (int rankIndex = 0; rankIndex < sortedMetrics.size(); rankIndex++) {
            rankByHandle.put(sortedMetrics.get(rankIndex).handle(), rankIndex + 1);
        }

        return rankByHandle;
    }

    private int calculateRankDelta(Integer currentRank, Integer baselineRank) {
        if (currentRank == null || baselineRank == null) {
            return 0;
        }

        return baselineRank - currentRank;
    }

    private boolean matchesHandle(RankListItemResult rank, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String normalizedQuery = query.trim().toLowerCase().replace("@", "");
        return rank.handle().toLowerCase().contains(normalizedQuery);
    }

    private Comparator<RankListItemResult> createRankComparator(RankSortKey rankSortKey) {
        if (rankSortKey == RankSortKey.AVG_EXECUTION_PERCENTILE) {
            return Comparator.comparingDouble(RankListItemResult::avgExecutionPercentile)
                    .thenComparing(Comparator.comparingInt(RankListItemResult::solvedCount).reversed())
                    .thenComparing(RankListItemResult::handle);
        }

        return Comparator.comparingInt(RankListItemResult::solvedCount)
                .reversed()
                .thenComparingDouble(RankListItemResult::avgExecutionPercentile)
                .thenComparing(RankListItemResult::handle);
    }

    private Comparator<RankMetrics> createRankMetricsComparator(RankSortKey rankSortKey) {
        if (rankSortKey == RankSortKey.AVG_EXECUTION_PERCENTILE) {
            return Comparator.comparingDouble(RankMetrics::avgExecutionPercentile)
                    .thenComparing(Comparator.comparingInt(RankMetrics::solvedCount).reversed())
                    .thenComparing(RankMetrics::handle);
        }

        return Comparator.comparingInt(RankMetrics::solvedCount)
                .reversed()
                .thenComparingDouble(RankMetrics::avgExecutionPercentile)
                .thenComparing(RankMetrics::handle);
    }

    private ProblemSolveHistory pickBetterHistory(ProblemSolveHistory currentHistory, ProblemSolveHistory candidateHistory) {
        if (candidateHistory.getCost() < currentHistory.getCost()) {
            return candidateHistory;
        }

        if (candidateHistory.getCost() > currentHistory.getCost()) {
            return currentHistory;
        }

        if (candidateHistory.getExecutionTimeMs() < currentHistory.getExecutionTimeMs()) {
            return candidateHistory;
        }

        if (candidateHistory.getExecutionTimeMs() > currentHistory.getExecutionTimeMs()) {
            return currentHistory;
        }

        if (candidateHistory.getSubmittedAt().isBefore(currentHistory.getSubmittedAt())) {
            return candidateHistory;
        }

        return currentHistory;
    }

    private DbmsType resolveDbmsType(String dbms) {
        if ("oracle".equalsIgnoreCase(dbms)) {
            return DbmsType.ORACLE;
        }

        return DbmsType.POSTGRESQL;
    }

    private DbmsType resolveDbmsType(ProblemSolveHistory history) {
        return history.getDbmsType() != null ? history.getDbmsType() : DbmsType.POSTGRESQL;
    }

    private RankSortKey resolveRankSortKey(String sortKey) {
        if ("avgExecutionPercentile".equalsIgnoreCase(sortKey)) {
            return RankSortKey.AVG_EXECUTION_PERCENTILE;
        }

        return RankSortKey.SOLVED_COUNT;
    }

    private record UserSolvedHistoryKey(String handle, String problemId) {
    }

    private record RankMetrics(String handle, int solvedCount, double avgExecutionPercentile) {
    }

    private enum RankSortKey {
        SOLVED_COUNT,
        AVG_EXECUTION_PERCENTILE
    }

}
