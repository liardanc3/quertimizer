package com.quertimizer.ranking.application.service;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.ranking.application.input.RankSearchInput;
import com.quertimizer.ranking.application.output.RankListItemOutput;
import com.quertimizer.ranking.application.output.RankMonthlyDeltaOutput;
import com.quertimizer.ranking.application.output.RankPageOutput;
import com.quertimizer.ranking.domain.model.RankPageConstant;
import com.quertimizer.ranking.domain.model.RankSortKey;
import com.quertimizer.ranking.domain.model.RankingSnapshot;
import com.quertimizer.ranking.domain.model.RankingSolveRecord;
import com.quertimizer.ranking.domain.model.RankingSubmitRecord;
import lombok.Value;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class RankingCalculator {

    public List<RankingSnapshot> calculateSnapshot(List<RankingSolveRecord> histories,
                                                   List<RankingSubmitRecord> submitHistories,
                                                   DbmsType dbmsType,
                                                   String snapshotId,
                                                   LocalDateTime calculatedAt) {
        // 현재와 월초 기준 랭킹 지표 계산
        LocalDateTime monthStart = calculatedAt.toLocalDate().withDayOfMonth(1).atStartOfDay();
        List<RankingSolveRecord> currentBestHistories = createBestHistories(histories, dbmsType, null);
        List<RankingSolveRecord> baselineBestHistories = createBestHistories(histories, dbmsType, monthStart);
        Map<String, RankMetrics> currentMetricsByHandle = createRankMetricsByHandle(currentBestHistories);
        Map<String, RankMetrics> baselineMetricsByHandle = createRankMetricsByHandle(baselineBestHistories);
        Map<String, SubmitMetrics> submitMetricsByHandle = createSubmitMetricsByHandle(submitHistories, dbmsType);

        // 정렬 기준별 고정 순위와 월간 변동폭 계산
        Map<String, Integer> solvedCountRankByHandle =
                createRankByHandle(currentMetricsByHandle.values().stream().toList(), RankSortKey.SOLVED_COUNT);
        Map<String, Integer> avgExecutionPercentileRankByHandle =
                createRankByHandle(currentMetricsByHandle.values().stream().toList(), RankSortKey.AVG_EXECUTION_PERCENTILE);
        Map<String, RankMonthlyDeltaOutput> monthlyDeltaByHandle =
                createMonthlyDeltaByHandle(currentMetricsByHandle, baselineMetricsByHandle);

        // 사용자별 snapshot record 생성
        List<RankingSnapshot> snapshots = new ArrayList<>();
        for (RankMetrics metrics : currentMetricsByHandle.values()) {
            SubmitMetrics submitMetrics = resolveSubmitMetrics(submitMetricsByHandle, metrics.handle());
            RankMonthlyDeltaOutput monthlyDelta = resolveMonthlyDelta(monthlyDeltaByHandle, metrics.handle());
            snapshots.add(new RankingSnapshot(
                    snapshotId, dbmsType, metrics.handle(),
                    metrics.solvedCount(), metrics.avgExecutionPercentile(),
                    submitMetrics.totalSubmitCount(), submitMetrics.successSubmitCount(),
                    solvedCountRankByHandle.get(metrics.handle()),
                    avgExecutionPercentileRankByHandle.get(metrics.handle()),
                    monthlyDelta.solvedCount(), monthlyDelta.avgExecutionPercentile(),
                    calculatedAt
            ));
        }

        return List.copyOf(snapshots);
    }

    public RankPageOutput createPage(List<RankingSnapshot> snapshots, RankSearchInput input) {
        // 검색어와 정렬 기준, 페이지 크기 확정
        int pageSize = resolvePageSize(input.getRequestedPageSize());
        RankSortKey rankSortKey = RankSortKey.fromValueOrDefault(input.getSortKey());
        List<RankingSnapshot> filteredSnapshots = snapshots.stream()
                .filter(snapshot -> matchesHandle(snapshot, input.getQuery()))
                .sorted(createSnapshotComparator(rankSortKey))
                .toList();

        // 요청 페이지 경계 보정 후 응답 생성
        int totalCount = filteredSnapshots.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) pageSize));
        int currentPage = Math.min(Math.max(input.getRequestedPage(), 1), totalPages);
        int fromIndex = Math.min((currentPage - 1) * pageSize, totalCount);
        int toIndex = Math.min(fromIndex + pageSize, totalCount);
        List<RankListItemOutput> ranks = filteredSnapshots.subList(fromIndex, toIndex).stream()
                .map(snapshot -> toRankListItem(snapshot, rankSortKey))
                .toList();

        return new RankPageOutput(currentPage, pageSize, totalCount, totalPages, ranks);
    }

    private int resolvePageSize(Integer requestedPageSize) {
        // 랭킹 페이지 크기를 보정
        if (requestedPageSize == null || requestedPageSize < 1) {
            return RankPageConstant.DEFAULT_PAGE_SIZE;
        }

        return Math.min(requestedPageSize, RankPageConstant.MAX_PAGE_SIZE);
    }

    private List<RankingSolveRecord> createBestHistories(List<RankingSolveRecord> histories,
                                                         DbmsType dbmsType,
                                                         LocalDateTime submittedBefore) {
        Map<UserSolvedHistoryKey, RankingSolveRecord> bestHistoryByKey = new HashMap<>();

        // 문제, 사용자 기준 최고 제출 추출
        for (RankingSolveRecord history : histories) {
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

    private Map<String, RankMetrics> createRankMetricsByHandle(List<RankingSolveRecord> bestHistories) {
        // Handle별 Rank 지표 생성
        Map<String, List<RankingSolveRecord>> historiesByProblemId = createHistoriesByProblemId(bestHistories);
        Map<UserSolvedHistoryKey, Double> costPercentileByHistoryKey =
                createCostPercentileByHistoryKey(historiesByProblemId);
        Map<String, List<Double>> costPercentilesByHandle = new HashMap<>();
        Map<String, Integer> solvedCountByHandle = new HashMap<>();

        // 사용자별 해결 수와 Cost 등수 백분위 수집
        for (RankingSolveRecord history : bestHistories) {
            String handle = history.getHandle();
            solvedCountByHandle.merge(handle, 1, Integer::sum);
            Double costPercentile = costPercentileByHistoryKey.get(
                    new UserSolvedHistoryKey(history.getHandle(), history.getProblemId())
            );

            if (costPercentile != null) {
                costPercentilesByHandle.computeIfAbsent(handle, key -> new ArrayList<>())
                        .add(costPercentile);
            }
        }

        Map<String, RankMetrics> metricsByHandle = new HashMap<>();
        for (Map.Entry<String, Integer> solvedCountEntry : solvedCountByHandle.entrySet()) {
            String handle = solvedCountEntry.getKey();
            List<Double> costPercentiles = costPercentilesByHandle.getOrDefault(handle, List.of());
            if (costPercentiles.isEmpty()) {
                continue;
            }

            double averageCostPercentile = costPercentiles.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0);
            metricsByHandle.put(
                    handle,
                    new RankMetrics(
                            handle, solvedCountEntry.getValue(),
                            Math.round(averageCostPercentile * 10d) / 10d
                    )
            );
        }

        return metricsByHandle;
    }

    private Map<String, List<RankingSolveRecord>> createHistoriesByProblemId(List<RankingSolveRecord> bestHistories) {
        // 문제 번호별 목록 생성
        Map<String, List<RankingSolveRecord>> historiesByProblemId = new HashMap<>();

        // 문제별 최고 제출 목록 구성
        for (RankingSolveRecord history : bestHistories) {
            historiesByProblemId.computeIfAbsent(history.getProblemId(), key -> new ArrayList<>())
                    .add(history);
        }

        return historiesByProblemId;
    }

    private Map<String, SubmitMetrics> createSubmitMetricsByHandle(List<RankingSubmitRecord> histories, DbmsType dbmsType) {
        // Handle별 전체 제출 수, 정답 제출 수 집계
        Map<String, SubmitMetrics> submitMetricsByHandle = new HashMap<>();

        for (RankingSubmitRecord history : histories) {
            if (resolveDbmsType(history) != dbmsType) {
                continue;
            }

            SubmitMetrics currentMetrics = submitMetricsByHandle.getOrDefault(history.getHandle(), new SubmitMetrics(0, 0));
            submitMetricsByHandle.put(
                    history.getHandle(),
                    new SubmitMetrics(
                            currentMetrics.totalSubmitCount() + 1,
                            currentMetrics.successSubmitCount() + (history.isSuccess() ? 1 : 0)
                    )
            );
        }

        return submitMetricsByHandle;
    }

    private SubmitMetrics resolveSubmitMetrics(Map<String, SubmitMetrics> submitMetricsByHandle, String handle) {
        // 사용자별 제출 지표가 없으면 빈 제출 지표 반환
        return submitMetricsByHandle.getOrDefault(handle, new SubmitMetrics(0, 0));
    }

    private RankMonthlyDeltaOutput resolveMonthlyDelta(Map<String, RankMonthlyDeltaOutput> monthlyDeltaByHandle, String handle) {
        // 월간 순위 변동 지표가 없으면 빈 변동 지표 반환
        return monthlyDeltaByHandle.getOrDefault(handle, new RankMonthlyDeltaOutput(0, 0));
    }

    private Map<UserSolvedHistoryKey, Double> createCostPercentileByHistoryKey(
            Map<String, List<RankingSolveRecord>> historiesByProblemId
    ) {
        Map<UserSolvedHistoryKey, Double> costPercentileByHistoryKey = new HashMap<>();

        // 문제별 Cost 등수 백분위 계산
        for (List<RankingSolveRecord> problemHistories : historiesByProblemId.values()) {
            List<RankingSolveRecord> sortedHistories = problemHistories.stream()
                    .sorted(
                            Comparator.comparingDouble(RankingSolveRecord::getCost)
                                    .thenComparing(RankingSolveRecord::getHandle)
                    )
                    .toList();

            Map<Double, Integer> rankByCost = new HashMap<>();
            for (int historyIndex = 0; historyIndex < sortedHistories.size(); historyIndex++) {
                rankByCost.putIfAbsent(sortedHistories.get(historyIndex).getCost(), historyIndex + 1);
            }

            int historyCount = sortedHistories.size();
            for (RankingSolveRecord history : sortedHistories) {
                int rank = rankByCost.getOrDefault(history.getCost(), 1);
                double costPercentile = historyCount <= 1
                        ? 0d
                        : ((rank - 1d) / (historyCount - 1d)) * 100d;
                costPercentileByHistoryKey.put(
                        new UserSolvedHistoryKey(history.getHandle(), history.getProblemId()),
                        costPercentile
                );
            }
        }

        return costPercentileByHistoryKey;
    }

    private Map<String, RankMonthlyDeltaOutput> createMonthlyDeltaByHandle(Map<String, RankMetrics> currentMetricsByHandle,
                                                                           Map<String, RankMetrics> baselineMetricsByHandle) {
        Map<String, Integer> currentSolvedCountRankByHandle =
                createRankByHandle(currentMetricsByHandle.values().stream().toList(), RankSortKey.SOLVED_COUNT);
        Map<String, Integer> baselineSolvedCountRankByHandle =
                createRankByHandle(baselineMetricsByHandle.values().stream().toList(), RankSortKey.SOLVED_COUNT);
        Map<String, Integer> currentExecutionPercentileRankByHandle =
                createRankByHandle(currentMetricsByHandle.values().stream().toList(), RankSortKey.AVG_EXECUTION_PERCENTILE);
        Map<String, Integer> baselineExecutionPercentileRankByHandle =
                createRankByHandle(baselineMetricsByHandle.values().stream().toList(), RankSortKey.AVG_EXECUTION_PERCENTILE);

        Map<String, RankMonthlyDeltaOutput> monthlyDeltaByHandle = new HashMap<>();
        for (RankMetrics currentMetrics : currentMetricsByHandle.values()) {
            String handle = currentMetrics.handle();
            monthlyDeltaByHandle.put(
                    handle,
                    new RankMonthlyDeltaOutput(
                            calculateRankDelta(currentSolvedCountRankByHandle.get(handle), baselineSolvedCountRankByHandle.get(handle)),
                            calculateRankDelta(currentExecutionPercentileRankByHandle.get(handle), baselineExecutionPercentileRankByHandle.get(handle))
                    )
            );
        }

        return monthlyDeltaByHandle;
    }

    private Map<String, Integer> createRankByHandle(List<RankMetrics> metrics, RankSortKey rankSortKey) {
        // Handle별 Rank 생성
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
        // Rank 변동폭 계산
        if (currentRank == null || baselineRank == null) {
            return 0;
        }

        return baselineRank - currentRank;
    }

    private boolean matchesHandle(RankingSnapshot snapshot, String query) {
        // Handle 일치 여부 확인
        if (query == null || query.isBlank()) {
            return true;
        }

        String normalizedQuery = query.trim().toLowerCase().replace("@", "");
        return snapshot.getHandle().toLowerCase().contains(normalizedQuery);
    }

    private RankListItemOutput toRankListItem(RankingSnapshot snapshot, RankSortKey rankSortKey) {
        // Snapshot record를 기존 랭킹 응답으로 변환
        return new RankListItemOutput(
                resolveRank(snapshot, rankSortKey),
                snapshot.getHandle(),
                snapshot.getSolvedCount(),
                snapshot.getAvgExecutionPercentile(),
                snapshot.getTotalSubmitCount(),
                snapshot.getSuccessSubmitCount(),
                new RankMonthlyDeltaOutput(
                        snapshot.getSolvedCountRankDelta(),
                        snapshot.getAvgExecutionPercentileRankDelta()
                )
        );
    }

    private int resolveRank(RankingSnapshot snapshot, RankSortKey rankSortKey) {
        // 정렬 기준별 표시 Rank 선택
        if (rankSortKey == RankSortKey.AVG_EXECUTION_PERCENTILE) {
            return snapshot.getAvgExecutionPercentileRank();
        }

        return snapshot.getSolvedCountRank();
    }

    private Comparator<RankingSnapshot> createSnapshotComparator(RankSortKey rankSortKey) {
        // Snapshot 조회 정렬 기준 생성
        if (rankSortKey == RankSortKey.AVG_EXECUTION_PERCENTILE) {
            return Comparator.comparingInt(RankingSnapshot::getAvgExecutionPercentileRank);
        }

        return Comparator.comparingInt(RankingSnapshot::getSolvedCountRank);
    }

    private Comparator<RankMetrics> createRankMetricsComparator(RankSortKey rankSortKey) {
        // Rank 지표 비교 기준 생성
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

    private RankingSolveRecord pickBetterHistory(RankingSolveRecord currentHistory, RankingSolveRecord candidateHistory) {
        // 더 나은 기록 선택
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

    private DbmsType resolveDbmsType(RankingSolveRecord history) {
        // 오래된 해결 이력은 PostgreSQL 기록으로 간주
        return history.getDbmsType() != null ? history.getDbmsType() : DbmsType.POSTGRESQL;
    }

    private DbmsType resolveDbmsType(RankingSubmitRecord history) {
        // 오래된 제출 이력은 PostgreSQL 기록으로 간주
        return history.getDbmsType() != null ? history.getDbmsType() : DbmsType.POSTGRESQL;
    }

    @Value
    @Accessors(fluent = true)
    private static class UserSolvedHistoryKey {
        // 사용자와 문제 조합으로 최고 해결 기록 식별
        String handle;
        String problemId;
    }

    @Value
    @Accessors(fluent = true)
    private static class RankMetrics {
        // 랭킹 정렬과 응답에 필요한 사용자별 지표 보관
        String handle;
        int solvedCount;
        double avgExecutionPercentile;
    }

    @Value
    @Accessors(fluent = true)
    private static class SubmitMetrics {
        // 랭킹 보조 정보로 표시할 제출 집계 보관
        int totalSubmitCount;
        int successSubmitCount;
    }
}
