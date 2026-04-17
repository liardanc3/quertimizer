package com.quertimizer.store;

import com.quertimizer.constant.DbmsType;
import com.quertimizer.entity.Problem;
import com.quertimizer.entity.ProblemSet;
import com.quertimizer.entity.ProblemSolveHistory;
import com.quertimizer.repository.ProblemRepository;
import com.quertimizer.repository.ProblemSetRepository;
import com.quertimizer.repository.ProblemSolveHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@RequiredArgsConstructor
public class ProblemStore {

    private static final int PROBLEM_PAGE_SIZE = 20;
    private static final double BYTES_PER_MB = 1024d * 1024d;

    private final ProblemRepository problemRepository;
    private final ProblemSetRepository problemSetRepository;
    private final ProblemSolveHistoryRepository problemSolveHistoryRepository;

    private final Map<String, Problem> problemsById = new ConcurrentHashMap<>();
    private final Map<String, ProblemSet> problemSetsById = new ConcurrentHashMap<>();
    private final Map<String, List<ProblemSolveHistory>> bestSubmittedHistoriesByProblemId = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void loadProblems() {

        // 문제 목록, 테이블셋 메모리 적재
        List<Problem> problems = problemRepository.findAll().stream()
                .sorted(Comparator.comparing(Problem::getProblemId))
                .toList();
        List<ProblemSet> problemSets = problemSetRepository.findAll().stream()
                .sorted(Comparator.comparing(ProblemSet::getProblemSetId))
                .toList();

        // 문제별 사용자 기준 최고 제출 추출
        Map<String, List<ProblemSolveHistory>> bestHistoriesByProblemId =
                createBestSubmittedHistoriesByProblemId(problemSolveHistoryRepository.findAll());

        problemsById.clear();
        problemSetsById.clear();
        bestSubmittedHistoriesByProblemId.clear();

        for (ProblemSet problemSet : problemSets) {
            problemSetsById.put(problemSet.getProblemSetId(), problemSet);
        }

        for (Problem problem : problems) {
            problemsById.put(problem.getProblemId(), problem);
            bestSubmittedHistoriesByProblemId.put(
                    problem.getProblemId(),
                    bestHistoriesByProblemId.getOrDefault(problem.getProblemId(), List.of())
            );
        }

        log.info("ProblemStore 로딩 완료 : {} MB", formatLoadedDataSizeInMb());
    }

    public ProblemPage findProblemPage(int requestedPage,
                                       String searchKeyword,
                                       String solveState,
                                       String currentUserId,
                                       boolean sortSolvedCountAscending,
                                       String spreadRateSort,
                                       Double spreadRateMin,
                                       Double spreadRateMax) {

        // 목록 필터와 정렬에 필요한 데이터 메모리 구성
        List<ProblemListEntry> searchableProblems = problemsById.values().stream()
                .map(problem -> createProblemListEntry(problem, currentUserId))
                .filter(problemEntry -> matchesSearch(problemEntry, searchKeyword))
                .filter(problemEntry -> matchesSolveState(problemEntry, solveState, currentUserId))
                .toList();
        SpreadRateBounds spreadRateBounds = createSpreadRateBounds(searchableProblems);
        SpreadRateFilter spreadRateFilter = createSpreadRateFilter(spreadRateMin, spreadRateMax);
        List<ProblemListEntry> filteredProblems = searchableProblems.stream()
                .filter(problemEntry -> matchesSpreadRate(problemEntry, spreadRateFilter))
                .sorted(createProblemComparator(sortSolvedCountAscending, spreadRateSort))
                .toList();

        // 페이지 경계 계산
        int totalCount = filteredProblems.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) PROBLEM_PAGE_SIZE));
        int currentPage = Math.min(Math.max(requestedPage, 1), totalPages);
        int fromIndex = Math.min((currentPage - 1) * PROBLEM_PAGE_SIZE, totalCount);
        int toIndex = Math.min(fromIndex + PROBLEM_PAGE_SIZE, totalCount);

        // 현재 페이지 데이터 반환
        return new ProblemPage(
                currentPage,
                PROBLEM_PAGE_SIZE,
                totalCount,
                totalPages,
                spreadRateBounds.min(),
                spreadRateBounds.max(),
                filteredProblems.subList(fromIndex, toIndex)
        );
    }

    public List<Problem> findAllProblems() {
        return problemsById.values().stream()
                .sorted(Comparator.comparing(Problem::getProblemId))
                .toList();
    }

    public List<ProblemSet> findAllProblemSets() {
        return problemSetsById.values().stream()
                .sorted(Comparator.comparing(ProblemSet::getProblemSetId))
                .toList();
    }

    public Optional<Problem> findProblem(String problemId) {
        return Optional.ofNullable(problemsById.get(problemId));
    }

    public Optional<ProblemSet> findProblemSet(String problemSetId) {
        return Optional.ofNullable(problemSetsById.get(problemSetId));
    }

    public List<ProblemSolveHistory> findBestSubmittedHistories(String problemId) {
        return bestSubmittedHistoriesByProblemId.getOrDefault(problemId, List.of());
    }

    private ProblemListEntry createProblemListEntry(Problem problem, String currentUserId) {
        List<ProblemSolveHistory> submittedHistories = findBestSubmittedHistories(problem.getProblemId());

        // 목록 통계 계산
        int solvedUserCount = (int) submittedHistories.stream()
                .map(ProblemSolveHistory::getUserId)
                .distinct()
                .count();

        // 현재 사용자 해결 여부 계산
        boolean solvedByCurrentUser = currentUserId != null
                && submittedHistories.stream().anyMatch(history -> history.getUserId().equals(currentUserId));

        return new ProblemListEntry(
                problem,
                submittedHistories,
                solvedUserCount,
                solvedByCurrentUser,
                calculateSpreadRate(submittedHistories)
        );
    }

    private boolean matchesSearch(ProblemListEntry problemEntry, String searchKeyword) {
        if (searchKeyword == null || searchKeyword.isBlank()) {
            return true;
        }

        String normalizedSearchKeyword = searchKeyword.trim().toLowerCase();
        return problemEntry.problem().getProblemId().toLowerCase().contains(normalizedSearchKeyword)
                || problemEntry.problem().getTitle().toLowerCase().contains(normalizedSearchKeyword);
    }

    private boolean matchesSolveState(ProblemListEntry problemEntry, String solveState, String currentUserId) {
        if (currentUserId == null || solveState == null || solveState.isBlank() || "all".equalsIgnoreCase(solveState)) {
            return true;
        }

        if ("none".equalsIgnoreCase(solveState)) {
            return false;
        }

        if ("solved".equalsIgnoreCase(solveState)) {
            return problemEntry.solvedByCurrentUser();
        }

        if ("unsolved".equalsIgnoreCase(solveState)) {
            return !problemEntry.solvedByCurrentUser();
        }

        return true;
    }

    private boolean matchesSpreadRate(ProblemListEntry problemEntry, SpreadRateFilter spreadRateFilter) {
        return problemEntry.spreadRate() >= spreadRateFilter.min()
                && problemEntry.spreadRate() <= spreadRateFilter.max();
    }

    private Comparator<ProblemListEntry> createProblemComparator(boolean sortSolvedCountAscending, String spreadRateSort) {
        Comparator<ProblemListEntry> solvedCountComparator = Comparator.comparingInt(ProblemListEntry::solvedUserCount);
        if (!sortSolvedCountAscending) {
            solvedCountComparator = solvedCountComparator.reversed();
        }

        if ("asc".equalsIgnoreCase(spreadRateSort)) {
            return Comparator.comparingDouble(ProblemListEntry::spreadRate)
                    .thenComparing(solvedCountComparator)
                    .thenComparing(problemEntry -> problemEntry.problem().getProblemId());
        }

        if ("desc".equalsIgnoreCase(spreadRateSort)) {
            return Comparator.comparingDouble(ProblemListEntry::spreadRate)
                    .reversed()
                    .thenComparing(solvedCountComparator)
                    .thenComparing(problemEntry -> problemEntry.problem().getProblemId());
        }

        return solvedCountComparator.thenComparing(problemEntry -> problemEntry.problem().getProblemId());
    }

    private SpreadRateBounds createSpreadRateBounds(List<ProblemListEntry> problemEntries) {
        if (problemEntries.isEmpty()) {
            return new SpreadRateBounds(0d, 0d);
        }

        return new SpreadRateBounds(
                roundToOneDecimal(problemEntries.stream().mapToDouble(ProblemListEntry::spreadRate).min().orElse(0d)),
                roundToOneDecimal(problemEntries.stream().mapToDouble(ProblemListEntry::spreadRate).max().orElse(0d))
        );
    }

    private SpreadRateFilter createSpreadRateFilter(Double spreadRateMin, Double spreadRateMax) {
        double min = spreadRateMin != null ? spreadRateMin : Double.NEGATIVE_INFINITY;
        double max = spreadRateMax != null ? spreadRateMax : Double.POSITIVE_INFINITY;

        if (min <= max) {
            return new SpreadRateFilter(min, max);
        }

        return new SpreadRateFilter(max, min);
    }

    private double calculateSpreadRate(List<ProblemSolveHistory> submittedHistories) {
        if (submittedHistories.isEmpty()) {
            return 0d;
        }

        List<Double> costs = submittedHistories.stream()
                .map(ProblemSolveHistory::getCost)
                .sorted()
                .toList();

        double min = costs.get(0);
        int size = costs.size();
        double median = (costs.get((size - 1) / 2) + costs.get(size / 2)) / 2d;
        double percentile90 = costs.get(Math.max(0, (int) Math.floor(size * 0.9d) - 1));

        return roundToOneDecimal(((percentile90 - min) / Math.max(Math.abs(median), 1d)) * 100d);
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10d) / 10d;
    }

    private Map<String, List<ProblemSolveHistory>> createBestSubmittedHistoriesByProblemId(List<ProblemSolveHistory> histories) {
        Map<ProblemSubmittedHistoryKey, ProblemSolveHistory> bestHistoryByKey = new HashMap<>();

        // 문제별 사용자 기준 최고 제출 집계
        for (ProblemSolveHistory history : histories) {
            ProblemSubmittedHistoryKey historyKey = new ProblemSubmittedHistoryKey(
                    history.getProblemId(),
                    history.getUserId()
            );

            bestHistoryByKey.merge(historyKey, history, this::pickBetterHistory);
        }
        Map<String, List<ProblemSolveHistory>> submittedHistoriesByProblemId = new HashMap<>();
        for (ProblemSolveHistory history : bestHistoryByKey.values()) {
            submittedHistoriesByProblemId.computeIfAbsent(history.getProblemId(), key -> new ArrayList<>())
                    .add(history);
        }

        // 응답용 정렬 순서 적용
        submittedHistoriesByProblemId.replaceAll((problemId, problemHistories) ->
                problemHistories.stream()
                        .sorted(
                                Comparator.comparingDouble(ProblemSolveHistory::getCost)
                                        .thenComparingLong(ProblemSolveHistory::getExecutionTimeMs)
                                        .thenComparing(ProblemSolveHistory::getUserId)
                        )
                        .toList()
        );
        return submittedHistoriesByProblemId;
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

    private DbmsType resolveDbmsType(ProblemSolveHistory history) {
        return history.getDbmsType() != null ? history.getDbmsType() : DbmsType.POSTGRESQL;
    }

    private String formatLoadedDataSizeInMb() {
        double loadedDataSizeInMb = calculateLoadedDataBytes() / BYTES_PER_MB;
        return "%.4f".formatted(loadedDataSizeInMb);
    }

    private long calculateLoadedDataBytes() {
        long problemBytes = problemsById.entrySet().stream()
                .mapToLong(entry -> measureString(entry.getKey()) + measureProblem(entry.getValue()))
                .sum();
        long problemSetBytes = problemSetsById.entrySet().stream()
                .mapToLong(entry -> measureString(entry.getKey()) + measureProblemSet(entry.getValue()))
                .sum();
        long historyBytes = bestSubmittedHistoriesByProblemId.entrySet().stream()
                .mapToLong(entry -> measureString(entry.getKey())
                        + entry.getValue().stream()
                        .mapToLong(this::measureProblemSolveHistory)
                        .sum())
                .sum();

        return problemBytes + problemSetBytes + historyBytes;
    }

    private long measureProblem(Problem problem) {
        return measureString(problem.getProblemId())
                + measureString(problem.getResolvedProblemSetId())
                + measureString(problem.getTitle())
                + measureString(problem.getDescription())
                + measureString(problem.getDdlPostgresql())
                + measureString(problem.getDdlOracle())
                + measureString(problem.getCondition())
                + measureString(problem.getOutput())
                + measureString(problem.getOutputSample())
                + measureString(problem.getAnswer());
    }

    private long measureProblemSet(ProblemSet problemSet) {
        return measureString(problemSet.getProblemSetId())
                + measureString(problemSet.getDdlPostgresql())
                + measureString(problemSet.getDdlOracle())
                + measureString(problemSet.getDataPostgresql())
                + measureString(problemSet.getDataOracle());
    }

    private long measureProblemSolveHistory(ProblemSolveHistory history) {
        return measureString(history.getProblemId())
                + measureString(history.getUserId())
                + measureString(resolveDbmsType(history).name())
                + measureString(history.getSubmittedSql())
                + Long.BYTES
                + Double.BYTES
                + Long.BYTES
                + Long.BYTES
                + measureString(history.getSubmittedAt().toString());
    }

    private long measureString(String value) {
        if (value == null) {
            return 0;
        }

        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private record ProblemSubmittedHistoryKey(String problemId, String userId) {
    }

    public record ProblemListEntry(Problem problem,
                                   List<ProblemSolveHistory> submittedHistories,
                                   int solvedUserCount,
                                   boolean solvedByCurrentUser,
                                   double spreadRate) {
    }

    public record ProblemPage(int currentPage,
                              int pageSize,
                              int totalCount,
                              int totalPages,
                              double spreadRateMin,
                              double spreadRateMax,
                              List<ProblemListEntry> problems) {
    }

    private record SpreadRateBounds(double min, double max) {
    }

    private record SpreadRateFilter(double min, double max) {
    }

}

