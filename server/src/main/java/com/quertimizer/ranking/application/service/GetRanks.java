package com.quertimizer.ranking.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.ranking.application.port.in.GetRanksUseCase;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.ranking.application.input.RankSearchInput;
import com.quertimizer.ranking.application.output.RankListItemOutput;
import com.quertimizer.ranking.application.output.RankMonthlyDeltaOutput;
import com.quertimizer.ranking.application.output.RankPageOutput;
import com.quertimizer.ranking.application.port.out.RankingProblemRecordPort;
import com.quertimizer.ranking.domain.model.RankPageConstant;
import com.quertimizer.ranking.domain.model.RankingSolveRecord;
import com.quertimizer.ranking.domain.model.RankingSubmitRecord;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.experimental.Accessors;
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
public class GetRanks implements GetRanksUseCase {

    private final RankingProblemRecordPort rankingProblemRecordPort;

    /**
     * 랭킹 검색 입력에 맞는 사용자 랭킹 페이지를 생성한다.
     *
     * <ol>
     *   <li>페이지 크기와 DBMS, 정렬 기준 확정
     *   <li>현재 및 월초 기준 랭킹 지표와 전역 순위 계산
     *   <li>검색, 정렬, 페이징 반영 응답 생성
     * </ol>
     *
     * @param input 랭킹 검색 조건
     */
    @Override
    @Log("랭킹 목록 조회")
    public RankPageOutput execute(RankSearchInput input) {
        int pageSize = resolvePageSize(input.getRequestedPageSize());
        DbmsType dbmsType = resolveDbmsType(input.getDbms());
        RankSortKey rankSortKey = resolveRankSortKey(input.getSortKey());
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        List<RankingSolveRecord> histories = rankingProblemRecordPort.findSolveRecords();
        List<RankingSubmitRecord> submitHistories = rankingProblemRecordPort.findSubmitRecords();

        List<RankingSolveRecord> currentBestHistories = createBestHistories(histories, dbmsType, null);
        List<RankingSolveRecord> baselineBestHistories = createBestHistories(histories, dbmsType, monthStart);

        Map<String, RankMetrics> currentMetricsByHandle = createRankMetricsByHandle(currentBestHistories);
        Map<String, RankMetrics> baselineMetricsByHandle = createRankMetricsByHandle(baselineBestHistories);
        Map<String, SubmitMetrics> submitMetricsByHandle = createSubmitMetricsByHandle(submitHistories, dbmsType);

        Map<String, RankMonthlyDeltaOutput> monthlyDeltaByHandle = createMonthlyDeltaByHandle(
                currentMetricsByHandle,
                baselineMetricsByHandle
        );
        Map<String, Integer> rankByHandle =
                createRankByHandle(currentMetricsByHandle.values().stream().toList(), rankSortKey);

        List<RankListItemOutput> filteredRanks = currentMetricsByHandle.values().stream()
                .map(metrics -> {
                    int rank = rankByHandle.get(metrics.handle());
                    SubmitMetrics submitMetrics = resolveSubmitMetrics(submitMetricsByHandle, metrics.handle());
                    return new RankListItemOutput(
                            rank, metrics.handle(), metrics.solvedCount(), metrics.avgExecutionPercentile(),
                            submitMetrics.totalSubmitCount(), submitMetrics.successSubmitCount(),
                            resolveMonthlyDelta(monthlyDeltaByHandle, metrics.handle())
                    );
                })
                .filter(rank -> matchesHandle(rank, input.getQuery()))
                .sorted(Comparator.comparingInt(RankListItemOutput::rank))
                .toList();

        int totalCount = filteredRanks.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) pageSize));
        int currentPage = Math.min(Math.max(input.getRequestedPage(), 1), totalPages);
        int fromIndex = Math.min((currentPage - 1) * pageSize, totalCount);
        int toIndex = Math.min(fromIndex + pageSize, totalCount);

        return new RankPageOutput(
                currentPage, pageSize, totalCount, totalPages,
                filteredRanks.subList(fromIndex, toIndex)
        );
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

            if (costPercentile == null) {
                continue;
            }

            costPercentilesByHandle.computeIfAbsent(handle, key -> new ArrayList<>())
                    .add(costPercentile);
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

    private boolean matchesHandle(RankListItemOutput rank, String query) {
        // Handle 일치 여부 확인
        if (query == null || query.isBlank()) {
            return true;
        }

        String normalizedQuery = query.trim().toLowerCase().replace("@", "");
        return rank.handle().toLowerCase().contains(normalizedQuery);
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

    private DbmsType resolveDbmsType(String dbms) {
        // 랭킹 조회 기준 DBMS를 기본값까지 포함해 확정
        return DbmsType.fromValueOrDefault(dbms, DbmsType.POSTGRESQL);
    }

    private DbmsType resolveDbmsType(RankingSolveRecord history) {
        // 오래된 해결 이력은 PostgreSQL 기록으로 간주
        return history.getDbmsType() != null ? history.getDbmsType() : DbmsType.POSTGRESQL;
    }

    private DbmsType resolveDbmsType(RankingSubmitRecord history) {
        // 오래된 제출 이력은 PostgreSQL 기록으로 간주
        return history.getDbmsType() != null ? history.getDbmsType() : DbmsType.POSTGRESQL;
    }

    private RankSortKey resolveRankSortKey(String sortKey) {
        // Rank 정렬 키 결정
        if ("avgExecutionPercentile".equalsIgnoreCase(sortKey)) {
            return RankSortKey.AVG_EXECUTION_PERCENTILE;
        }

        return RankSortKey.SOLVED_COUNT;
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

    private enum RankSortKey {
        SOLVED_COUNT,
        AVG_EXECUTION_PERCENTILE
    }

}
