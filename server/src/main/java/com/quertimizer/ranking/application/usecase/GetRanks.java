package com.quertimizer.ranking.application.usecase;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.problem.application.port.ProblemSubmitHistoryRepository;
import com.quertimizer.problem.domain.entity.ProblemSubmitHistory;
import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import com.quertimizer.problem.application.port.ProblemSolveHistoryRepository;
import com.quertimizer.ranking.application.output.RankListItemOutput;
import com.quertimizer.ranking.application.output.RankMonthlyDeltaOutput;
import com.quertimizer.ranking.application.output.RankPageOutput;
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

    private static final int DEFAULT_RANK_PAGE_SIZE = 10;
    private static final int MAX_RANK_PAGE_SIZE = 100;
    private static final RankMonthlyDeltaOutput EMPTY_MONTHLY_DELTA = new RankMonthlyDeltaOutput(0, 0);
    private static final SubmitMetrics EMPTY_SUBMIT_METRICS = new SubmitMetrics(0, 0);

    private final ProblemSolveHistoryRepository problemSolveHistoryRepository;
    private final ProblemSubmitHistoryRepository problemSubmitHistoryRepository;

    public RankPageOutput execute(int requestedPage, Integer requestedPageSize, String dbms, String query, String sortKey) {
        // 랭킹 페이지 데이터를 조회
        int pageSize = resolvePageSize(requestedPageSize);
        DbmsType dbmsType = resolveDbmsType(dbms);
        RankSortKey rankSortKey = resolveRankSortKey(sortKey);
        LocalDateTime monthStart = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        List<ProblemSolveHistory> histories = problemSolveHistoryRepository.findAll();
        List<ProblemSubmitHistory> submitHistories = problemSubmitHistoryRepository.findAll();

        // 현재 기준 최고 제출 이력 추출
        List<ProblemSolveHistory> currentBestHistories = createBestHistories(histories, dbmsType, null);

        // 이번달 1일 기준 최고 제출 이력 추출
        List<ProblemSolveHistory> baselineBestHistories = createBestHistories(histories, dbmsType, monthStart);

        // 사용자별 랭킹 지표 계산
        Map<String, RankMetrics> currentMetricsByHandle = createRankMetricsByHandle(currentBestHistories);
        Map<String, RankMetrics> baselineMetricsByHandle = createRankMetricsByHandle(baselineBestHistories);
        Map<String, SubmitMetrics> submitMetricsByHandle = createSubmitMetricsByHandle(submitHistories, dbmsType);

        // 이번달 1일 대비 순위 변화 계산
        Map<String, RankMonthlyDeltaOutput> monthlyDeltaByHandle = createMonthlyDeltaByHandle(
                currentMetricsByHandle,
                baselineMetricsByHandle
        );

        // 검색, 정렬 반영 랭킹 목록 구성
        List<RankListItemOutput> filteredRanks = currentMetricsByHandle.values().stream()
                .map(metrics -> new RankListItemOutput(
                        metrics.handle(),
                        metrics.solvedCount(),
                        metrics.avgExecutionPercentile(),
                        submitMetricsByHandle.getOrDefault(metrics.handle(), EMPTY_SUBMIT_METRICS).totalSubmitCount(),
                        submitMetricsByHandle.getOrDefault(metrics.handle(), EMPTY_SUBMIT_METRICS).successSubmitCount(),
                        monthlyDeltaByHandle.getOrDefault(metrics.handle(), EMPTY_MONTHLY_DELTA)
                ))
                .filter(rank -> matchesHandle(rank, query))
                .sorted(createRankComparator(rankSortKey))
                .toList();

        // 페이지 경계 계산
        int totalCount = filteredRanks.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) pageSize));
        int currentPage = Math.min(Math.max(requestedPage, 1), totalPages);
        int fromIndex = Math.min((currentPage - 1) * pageSize, totalCount);
        int toIndex = Math.min(fromIndex + pageSize, totalCount);

        return new RankPageOutput(
                currentPage,
                pageSize,
                totalCount,
                totalPages,
                filteredRanks.subList(fromIndex, toIndex)
        );
    }

    private int resolvePageSize(Integer requestedPageSize) {
        // 랭킹 페이지 크기를 보정
        if (requestedPageSize == null || requestedPageSize < 1) {
            return DEFAULT_RANK_PAGE_SIZE;
        }

        return Math.min(requestedPageSize, MAX_RANK_PAGE_SIZE);
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
        // Handle별 Rank 지표 생성
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
        // 문제 번호별 목록 생성
        Map<String, List<ProblemSolveHistory>> historiesByProblemId = new HashMap<>();

        // 문제별 최고 제출 목록 구성
        for (ProblemSolveHistory history : bestHistories) {
            historiesByProblemId.computeIfAbsent(history.getProblemId(), key -> new ArrayList<>())
                    .add(history);
        }

        return historiesByProblemId;
    }

    private Map<String, SubmitMetrics> createSubmitMetricsByHandle(List<ProblemSubmitHistory> histories, DbmsType dbmsType) {
        // Handle별 전체 제출 수, 정답 제출 수 집계
        Map<String, SubmitMetrics> submitMetricsByHandle = new HashMap<>();

        for (ProblemSubmitHistory history : histories) {
            if (resolveDbmsType(history) != dbmsType) {
                continue;
            }

            SubmitMetrics currentMetrics = submitMetricsByHandle.getOrDefault(history.getHandle(), EMPTY_SUBMIT_METRICS);
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

    private Comparator<RankListItemOutput> createRankComparator(RankSortKey rankSortKey) {
        // Rank 비교 기준 생성
        if (rankSortKey == RankSortKey.AVG_EXECUTION_PERCENTILE) {
            return Comparator.comparingDouble(RankListItemOutput::avgExecutionPercentile)
                    .thenComparing(Comparator.comparingInt(RankListItemOutput::solvedCount).reversed())
                    .thenComparing(RankListItemOutput::handle);
        }

        return Comparator.comparingInt(RankListItemOutput::solvedCount)
                .reversed()
                .thenComparingDouble(RankListItemOutput::avgExecutionPercentile)
                .thenComparing(RankListItemOutput::handle);
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

    private ProblemSolveHistory pickBetterHistory(ProblemSolveHistory currentHistory, ProblemSolveHistory candidateHistory) {
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
        // DBMS 유형 결정
        return DbmsType.fromValueOrDefault(dbms, DbmsType.POSTGRESQL);
    }

    private DbmsType resolveDbmsType(ProblemSolveHistory history) {
        // DBMS 유형 결정
        return history.getDbmsType() != null ? history.getDbmsType() : DbmsType.POSTGRESQL;
    }

    private DbmsType resolveDbmsType(ProblemSubmitHistory history) {
        // DBMS 유형 결정
        return history.getDbmsType() != null ? history.getDbmsType() : DbmsType.POSTGRESQL;
    }

    private RankSortKey resolveRankSortKey(String sortKey) {
        // Rank 정렬 키 결정
        if ("avgExecutionPercentile".equalsIgnoreCase(sortKey)) {
            return RankSortKey.AVG_EXECUTION_PERCENTILE;
        }

        return RankSortKey.SOLVED_COUNT;
    }

    private record UserSolvedHistoryKey(String handle, String problemId) {
        // 사용자 해결한 기록 키 처리
    }

    private record RankMetrics(String handle, int solvedCount, double avgExecutionPercentile) {
        // Rank 지표 처리
    }

    private record SubmitMetrics(int totalSubmitCount, int successSubmitCount) {
        // 제출 지표 처리
    }

    private enum RankSortKey {
        SOLVED_COUNT,
        AVG_EXECUTION_PERCENTILE
    }

}
