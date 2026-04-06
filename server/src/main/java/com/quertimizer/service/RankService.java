package com.quertimizer.service;

import com.quertimizer.constant.DbmsType;
import com.quertimizer.endpoint.api.dto.response.RankListItemRes;
import com.quertimizer.endpoint.api.dto.response.RankMonthlyDeltaRes;
import com.quertimizer.endpoint.api.dto.response.RankPageRes;
import com.quertimizer.entity.ProblemSolveHistory;
import com.quertimizer.repository.ProblemSolveHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RankService {

    private static final int RANK_PAGE_SIZE = 100;
    private static final RankMonthlyDeltaRes EMPTY_MONTHLY_DELTA = new RankMonthlyDeltaRes(0, 0);

    private final ProblemSolveHistoryRepository problemSolveHistoryRepository;

    public RankPageRes getRanks(int requestedPage, String dbms, String query, String sortKey) {
        DbmsType dbmsType = resolveDbmsType(dbms);
        RankSortKey rankSortKey = resolveRankSortKey(sortKey);
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        List<ProblemSolveHistory> histories = problemSolveHistoryRepository.findAll();

        // 현재 기준 최고 제출 이력 추출
        List<ProblemSolveHistory> currentBestHistories = createBestHistories(histories, dbmsType, null);

        // 이번달 1일 기준 최고 제출 이력 추출
        List<ProblemSolveHistory> baselineBestHistories = createBestHistories(histories, dbmsType, monthStart);

        // 사용자별 랭킹 지표 계산
        Map<String, RankMetrics> currentMetricsByUserId = createRankMetricsByUserId(currentBestHistories);
        Map<String, RankMetrics> baselineMetricsByUserId = createRankMetricsByUserId(baselineBestHistories);

        // 이번달 1일 대비 순위 변화 계산
        Map<String, RankMonthlyDeltaRes> monthlyDeltaByUserId = createMonthlyDeltaByUserId(
                currentMetricsByUserId,
                baselineMetricsByUserId
        );

        // 검색, 정렬 반영 랭킹 목록 구성
        List<RankListItemRes> filteredRanks = currentMetricsByUserId.values().stream()
                .map(metrics -> new RankListItemRes(
                        metrics.userId(),
                        metrics.solvedCount(),
                        metrics.avgExecutionPercentile(),
                        monthlyDeltaByUserId.getOrDefault(metrics.userId(), EMPTY_MONTHLY_DELTA)
                ))
                .filter(rank -> matchesUserId(rank, query))
                .sorted(createRankComparator(rankSortKey))
                .toList();

        // 페이지 경계 계산
        int totalCount = filteredRanks.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) RANK_PAGE_SIZE));
        int currentPage = Math.min(Math.max(requestedPage, 1), totalPages);
        int fromIndex = Math.min((currentPage - 1) * RANK_PAGE_SIZE, totalCount);
        int toIndex = Math.min(fromIndex + RANK_PAGE_SIZE, totalCount);

        return new RankPageRes(
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

            UserSolvedHistoryKey historyKey = new UserSolvedHistoryKey(history.getUserId(), history.getProblemId());
            bestHistoryByKey.merge(historyKey, history, this::pickFasterHistory);
        }

        return bestHistoryByKey.values().stream().toList();
    }

    private Map<String, RankMetrics> createRankMetricsByUserId(List<ProblemSolveHistory> bestHistories) {
        Map<String, List<ProblemSolveHistory>> historiesByProblemId = createHistoriesByProblemId(bestHistories);
        Map<UserSolvedHistoryKey, Integer> executionPercentileByHistoryKey =
                createExecutionPercentileByHistoryKey(historiesByProblemId);
        Map<String, List<Integer>> executionPercentilesByUserId = new HashMap<>();
        Map<String, Integer> solvedCountByUserId = new HashMap<>();

        // 사용자별 해결 수, 실행시간 백분위 수집
        for (ProblemSolveHistory history : bestHistories) {
            String userId = history.getUserId();
            solvedCountByUserId.merge(userId, 1, Integer::sum);

            Integer executionPercentile = executionPercentileByHistoryKey.get(
                    new UserSolvedHistoryKey(history.getUserId(), history.getProblemId())
            );

            if (executionPercentile == null) {
                continue;
            }

            executionPercentilesByUserId.computeIfAbsent(userId, key -> new ArrayList<>())
                    .add(executionPercentile);
        }

        Map<String, RankMetrics> metricsByUserId = new HashMap<>();
        for (Map.Entry<String, Integer> solvedCountEntry : solvedCountByUserId.entrySet()) {
            String userId = solvedCountEntry.getKey();
            List<Integer> executionPercentiles = executionPercentilesByUserId.getOrDefault(userId, List.of());

            if (executionPercentiles.isEmpty()) {
                continue;
            }

            double averageExecutionPercentile = executionPercentiles.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0);

            metricsByUserId.put(
                    userId,
                    new RankMetrics(
                            userId,
                            solvedCountEntry.getValue(),
                            Math.round(averageExecutionPercentile * 10d) / 10d
                    )
            );
        }

        return metricsByUserId;
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
                                    .thenComparing(ProblemSolveHistory::getUserId)
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
                        new UserSolvedHistoryKey(history.getUserId(), history.getProblemId()),
                        executionPercentile
                );
            }
        }

        return executionPercentileByHistoryKey;
    }

    private Map<String, RankMonthlyDeltaRes> createMonthlyDeltaByUserId(Map<String, RankMetrics> currentMetricsByUserId,
                                                                        Map<String, RankMetrics> baselineMetricsByUserId) {
        Map<String, Integer> currentSolvedCountRankByUserId =
                createRankByUserId(currentMetricsByUserId.values().stream().toList(), RankSortKey.SOLVED_COUNT);
        Map<String, Integer> baselineSolvedCountRankByUserId =
                createRankByUserId(baselineMetricsByUserId.values().stream().toList(), RankSortKey.SOLVED_COUNT);
        Map<String, Integer> currentExecutionPercentileRankByUserId =
                createRankByUserId(currentMetricsByUserId.values().stream().toList(), RankSortKey.AVG_EXECUTION_PERCENTILE);
        Map<String, Integer> baselineExecutionPercentileRankByUserId =
                createRankByUserId(baselineMetricsByUserId.values().stream().toList(), RankSortKey.AVG_EXECUTION_PERCENTILE);

        Map<String, RankMonthlyDeltaRes> monthlyDeltaByUserId = new HashMap<>();
        for (RankMetrics currentMetrics : currentMetricsByUserId.values()) {
            String userId = currentMetrics.userId();

            monthlyDeltaByUserId.put(
                    userId,
                    new RankMonthlyDeltaRes(
                            calculateRankDelta(
                                    currentSolvedCountRankByUserId.get(userId),
                                    baselineSolvedCountRankByUserId.get(userId)
                            ),
                            calculateRankDelta(
                                    currentExecutionPercentileRankByUserId.get(userId),
                                    baselineExecutionPercentileRankByUserId.get(userId)
                            )
                    )
            );
        }

        return monthlyDeltaByUserId;
    }

    private Map<String, Integer> createRankByUserId(List<RankMetrics> metrics, RankSortKey rankSortKey) {
        Map<String, Integer> rankByUserId = new HashMap<>();
        List<RankMetrics> sortedMetrics = metrics.stream()
                .sorted(createRankMetricsComparator(rankSortKey))
                .toList();

        // 정렬 결과 기준 순위 부여
        for (int rankIndex = 0; rankIndex < sortedMetrics.size(); rankIndex++) {
            rankByUserId.put(sortedMetrics.get(rankIndex).userId(), rankIndex + 1);
        }

        return rankByUserId;
    }

    private int calculateRankDelta(Integer currentRank, Integer baselineRank) {
        if (currentRank == null || baselineRank == null) {
            return 0;
        }

        return baselineRank - currentRank;
    }

    private boolean matchesUserId(RankListItemRes rank, String query) {
        if (query == null || query.isBlank()) {
            return true;
        }

        String normalizedQuery = query.trim().toLowerCase().replace("@", "");
        return rank.getUserId().toLowerCase().contains(normalizedQuery);
    }

    private Comparator<RankListItemRes> createRankComparator(RankSortKey rankSortKey) {
        if (rankSortKey == RankSortKey.AVG_EXECUTION_PERCENTILE) {
            return Comparator.comparingDouble(RankListItemRes::getAvgExecutionPercentile)
                    .thenComparing(Comparator.comparingInt(RankListItemRes::getSolvedCount).reversed())
                    .thenComparing(RankListItemRes::getUserId);
        }

        return Comparator.comparingInt(RankListItemRes::getSolvedCount)
                .reversed()
                .thenComparingDouble(RankListItemRes::getAvgExecutionPercentile)
                .thenComparing(RankListItemRes::getUserId);
    }

    private Comparator<RankMetrics> createRankMetricsComparator(RankSortKey rankSortKey) {
        if (rankSortKey == RankSortKey.AVG_EXECUTION_PERCENTILE) {
            return Comparator.comparingDouble(RankMetrics::avgExecutionPercentile)
                    .thenComparing(Comparator.comparingInt(RankMetrics::solvedCount).reversed())
                    .thenComparing(RankMetrics::userId);
        }

        return Comparator.comparingInt(RankMetrics::solvedCount)
                .reversed()
                .thenComparingDouble(RankMetrics::avgExecutionPercentile)
                .thenComparing(RankMetrics::userId);
    }

    private ProblemSolveHistory pickFasterHistory(ProblemSolveHistory currentHistory, ProblemSolveHistory candidateHistory) {
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

    private record UserSolvedHistoryKey(String userId, String problemId) {
    }

    private record RankMetrics(String userId, int solvedCount, double avgExecutionPercentile) {
    }

    private enum RankSortKey {
        SOLVED_COUNT,
        AVG_EXECUTION_PERCENTILE
    }

}
