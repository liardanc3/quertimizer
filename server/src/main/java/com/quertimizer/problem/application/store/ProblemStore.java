package com.quertimizer.problem.application.store;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.domain.entity.ProblemSet;
import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import com.quertimizer.problem.domain.entity.ProblemSubmitHistory;
import com.quertimizer.problem.application.port.ProblemRepository;
import com.quertimizer.problem.application.port.ProblemSetRepository;
import com.quertimizer.problem.application.port.ProblemSolveHistoryRepository;
import com.quertimizer.problem.application.port.ProblemSubmitHistoryRepository;
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

    private static final int PROBLEM_PAGE_SIZE = 10;
    private static final double BYTES_PER_MB = 1024d * 1024d;

    private final ProblemRepository problemRepository;
    private final ProblemSetRepository problemSetRepository;
    private final ProblemSolveHistoryRepository problemSolveHistoryRepository;
    private final ProblemSubmitHistoryRepository problemSubmitHistoryRepository;

    private final Map<String, Problem> problemsById = new ConcurrentHashMap<>();
    private final Map<String, ProblemSet> problemSetsById = new ConcurrentHashMap<>();
    private final Map<String, List<ProblemSolveHistory>> bestSubmittedHistoriesByProblemId = new ConcurrentHashMap<>();
    private final Map<String, ProblemSubmissionStats> submissionStatsByProblemId = new ConcurrentHashMap<>();

    @EventListener(ApplicationReadyEvent.class)
    public void loadProblems() {

        // 문제 목록, 테이블셋 메모리 적재
        List<Problem> problems = problemRepository.findAll().stream()
                .filter(Problem::hasSupportedDbms)
                .sorted(Comparator.comparing(Problem::getProblemId))
                .toList();
        List<ProblemSet> problemSets = problemSetRepository.findAll().stream()
                .filter(ProblemSet::hasSupportedDbms)
                .sorted(Comparator.comparing(ProblemSet::getProblemSetId))
                .toList();

        // 문제별 사용자 기준 최고 제출 추출
        Map<String, List<ProblemSolveHistory>> bestHistoriesByProblemId =
                createBestSubmittedHistoriesByProblemId(problemSolveHistoryRepository.findAll());
        Map<String, ProblemSubmissionStats> submissionStats =
                createSubmissionStatsByProblemId(problemSubmitHistoryRepository.findAll());

        problemsById.clear();
        problemSetsById.clear();
        bestSubmittedHistoriesByProblemId.clear();
        submissionStatsByProblemId.clear();

        for (ProblemSet problemSet : problemSets) {
            problemSetsById.put(problemSet.getProblemSetId(), problemSet);
        }

        for (Problem problem : problems) {
            problemsById.put(problem.getProblemId(), problem);
            bestSubmittedHistoriesByProblemId.put(
                    problem.getProblemId(),
                    bestHistoriesByProblemId.getOrDefault(problem.getProblemId(), List.of())
            );
            submissionStatsByProblemId.put(
                    problem.getProblemId(),
                    submissionStats.getOrDefault(problem.getProblemId(), ProblemSubmissionStats.empty())
            );
        }

        log.info("ProblemStore 로딩 완료 : {} MB", formatLoadedDataSizeInMb());
    }

    public ProblemPage findProblemPage(int requestedPage,
                                       String searchKeyword,
                                       DbmsType dbmsType,
                                       String solveState,
                                       String currentHandle,
                                       String solvedCountSort,
                                       String totalSubmitSort,
                                       String successSubmitSort,
                                       String spreadRateSort,
                                       Double spreadRateMin,
                                       Double spreadRateMax) {

        // 목록 필터와 정렬에 필요한 데이터 메모리 구성
        List<ProblemListEntry> searchableProblems = problemsById.values().stream()
                .filter(problem -> problem.supportsDbms(dbmsType))
                .map(problem -> createProblemListEntry(problem, currentHandle))
                .filter(problemEntry -> matchesSearch(problemEntry, searchKeyword))
                .filter(problemEntry -> matchesSolveState(problemEntry, solveState, currentHandle))
                .toList();
        SpreadRateBounds spreadRateBounds = createSpreadRateBounds(searchableProblems);
        SpreadRateFilter spreadRateFilter = createSpreadRateFilter(spreadRateMin, spreadRateMax);
        List<ProblemListEntry> filteredProblems = searchableProblems.stream()
                .filter(problemEntry -> matchesSpreadRate(problemEntry, spreadRateFilter))
                .sorted(createProblemComparator(solvedCountSort, totalSubmitSort, successSubmitSort, spreadRateSort))
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
        // 전체 문제 목록 조회
        return problemsById.values().stream()
                .sorted(Comparator.comparing(Problem::getProblemId))
                .toList();
    }

    public List<ProblemSet> findAllProblemSets() {
        // 전체 문제 테이블셋 s 조회
        return problemSetsById.values().stream()
                .sorted(Comparator.comparing(ProblemSet::getProblemSetId))
                .toList();
    }

    public Optional<Problem> findProblem(String problemId) {
        // 문제 조회
        return Optional.ofNullable(problemsById.get(problemId));
    }

    public Optional<ProblemSet> findProblemSet(String problemSetId) {
        // 문제 테이블셋 조회
        return Optional.ofNullable(problemSetsById.get(problemSetId));
    }

    public List<ProblemSolveHistory> findBestSubmittedHistories(String problemId) {
        // 최고 제출 목록 조회
        return bestSubmittedHistoriesByProblemId.getOrDefault(problemId, List.of());
    }

    private ProblemListEntry createProblemListEntry(Problem problem, String currentHandle) {
        // 문제 목록 Entry 생성
        List<ProblemSolveHistory> submittedHistories = findBestSubmittedHistories(problem.getProblemId());
        ProblemSubmissionStats submissionStats = submissionStatsByProblemId.getOrDefault(
                problem.getProblemId(),
                ProblemSubmissionStats.empty()
        );

        // 목록 통계 계산
        int solvedUserCount = (int) submittedHistories.stream()
                .map(ProblemSolveHistory::getHandle)
                .distinct()
                .count();

        // 현재 사용자 해결 여부 계산
        boolean solvedByCurrentUser = currentHandle != null
                && submittedHistories.stream().anyMatch(history -> history.getHandle().equals(currentHandle));

        return new ProblemListEntry(
                problem,
                submittedHistories,
                solvedUserCount,
                solvedByCurrentUser,
                submissionStats.totalSubmitCount(),
                submissionStats.successSubmitCount(),
                calculateSpreadRate(submittedHistories)
        );
    }

    private boolean matchesSearch(ProblemListEntry problemEntry, String searchKeyword) {
        // 검색 일치 여부 확인
        if (searchKeyword == null || searchKeyword.isBlank()) {
            return true;
        }

        String normalizedSearchKeyword = searchKeyword.trim().toLowerCase();
        return problemEntry.problem().getProblemId().toLowerCase().contains(normalizedSearchKeyword)
                || problemEntry.problem().getTitle().toLowerCase().contains(normalizedSearchKeyword);
    }

    private boolean matchesSolveState(ProblemListEntry problemEntry, String solveState, String currentHandle) {
        // Solve State 일치 여부 확인
        if (currentHandle == null || solveState == null || solveState.isBlank() || "all".equalsIgnoreCase(solveState)) {
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
        // Spread Rate 일치 여부 확인
        return problemEntry.spreadRate() >= spreadRateFilter.min()
                && problemEntry.spreadRate() <= spreadRateFilter.max();
    }

    private Comparator<ProblemListEntry> createProblemComparator(String solvedCountSort,
                                                             String totalSubmitSort,
                                                             String successSubmitSort,
                                                             String spreadRateSort) {
        Comparator<ProblemListEntry> problemIdComparator = Comparator.comparing(problemEntry -> problemEntry.problem().getProblemId());

        if ("asc".equalsIgnoreCase(spreadRateSort)) {
            return Comparator.comparingDouble(ProblemListEntry::spreadRate)
                    .thenComparing(problemIdComparator);
        }

        if ("desc".equalsIgnoreCase(spreadRateSort)) {
            return Comparator.comparingDouble(ProblemListEntry::spreadRate)
                    .reversed()
                    .thenComparing(problemIdComparator);
        }

        if ("asc".equalsIgnoreCase(totalSubmitSort)) {
            return Comparator.comparingInt(ProblemListEntry::totalSubmitCount)
                    .thenComparing(problemIdComparator);
        }

        if ("desc".equalsIgnoreCase(totalSubmitSort)) {
            return Comparator.comparingInt(ProblemListEntry::totalSubmitCount)
                    .reversed()
                    .thenComparing(problemIdComparator);
        }

        if ("asc".equalsIgnoreCase(successSubmitSort)) {
            return Comparator.comparingInt(ProblemListEntry::successSubmitCount)
                    .thenComparing(problemIdComparator);
        }

        if ("desc".equalsIgnoreCase(successSubmitSort)) {
            return Comparator.comparingInt(ProblemListEntry::successSubmitCount)
                    .reversed()
                    .thenComparing(problemIdComparator);
        }

        if ("asc".equalsIgnoreCase(solvedCountSort)) {
            return Comparator.comparingInt(ProblemListEntry::solvedUserCount)
                    .thenComparing(problemIdComparator);
        }

        return Comparator.comparingInt(ProblemListEntry::solvedUserCount)
                .reversed()
                .thenComparing(problemIdComparator);
    }

    private SpreadRateBounds createSpreadRateBounds(List<ProblemListEntry> problemEntries) {
        // Spread Rate Bounds 생성
        if (problemEntries.isEmpty()) {
            return new SpreadRateBounds(0d, 0d);
        }

        return new SpreadRateBounds(
                roundToOneDecimal(problemEntries.stream().mapToDouble(ProblemListEntry::spreadRate).min().orElse(0d)),
                roundToOneDecimal(problemEntries.stream().mapToDouble(ProblemListEntry::spreadRate).max().orElse(0d))
        );
    }

    private SpreadRateFilter createSpreadRateFilter(Double spreadRateMin, Double spreadRateMax) {
        // Spread Rate 필터 생성
        double min = spreadRateMin != null ? spreadRateMin : Double.NEGATIVE_INFINITY;
        double max = spreadRateMax != null ? spreadRateMax : Double.POSITIVE_INFINITY;

        if (min <= max) {
            return new SpreadRateFilter(min, max);
        }

        return new SpreadRateFilter(max, min);
    }

    private double calculateSpreadRate(List<ProblemSolveHistory> submittedHistories) {
        // Spread Rate 계산
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
        // round To One Decimal 처리
        return Math.round(value * 10d) / 10d;
    }

    private Map<String, List<ProblemSolveHistory>> createBestSubmittedHistoriesByProblemId(List<ProblemSolveHistory> histories) {
        // 문제 번호별 최고 제출 목록 생성
        Map<ProblemSubmittedHistoryKey, ProblemSolveHistory> bestHistoryByKey = new HashMap<>();

        // 문제별 사용자 기준 최고 제출 집계
        for (ProblemSolveHistory history : histories) {
            ProblemSubmittedHistoryKey historyKey = new ProblemSubmittedHistoryKey(
                    history.getProblemId(),
                    history.getHandle()
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
                                        .thenComparing(ProblemSolveHistory::getHandle)
                        )
                        .toList()
        );
        return submittedHistoriesByProblemId;
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

    private Map<String, ProblemSubmissionStats> createSubmissionStatsByProblemId(List<ProblemSubmitHistory> histories) {
        // 문제 번호별 Submission Stats 생성
        Map<String, ProblemSubmissionStats> submissionStatsByProblemId = new HashMap<>();

        for (ProblemSubmitHistory history : histories) {
            submissionStatsByProblemId.merge(
                    history.getProblemId(),
                    ProblemSubmissionStats.from(history.isSuccess()),
                    ProblemSubmissionStats::merge
            );
        }

        return submissionStatsByProblemId;
    }

    private DbmsType resolveDbmsType(ProblemSolveHistory history) {
        // 요청 DBMS 값을 내부 유형으로 맞춘다
        return history.getDbmsType() != null ? history.getDbmsType() : DbmsType.POSTGRESQL;
    }

    private String formatLoadedDataSizeInMb() {
        // Loaded 데이터 크기 In Mb 포맷
        double loadedDataSizeInMb = calculateLoadedDataBytes() / BYTES_PER_MB;
        return "%.4f".formatted(loadedDataSizeInMb);
    }

    private long calculateLoadedDataBytes() {
        // Loaded 데이터 Bytes 계산
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
        // measure 문제 처리
        return measureString(problem.getProblemId())
                + measureString(problem.getResolvedProblemSetId())
                + measureString(problem.getTitle())
                + measureString(problem.getDescription())
                + measureString(problem.getDdl())
                + measureString(problem.getCondition())
                + measureString(problem.getOutput())
                + measureString(problem.getOutputSample())
                + measureString(problem.getAnswer())
                + measureString(problem.getAnswerSql());
    }

    private long measureProblemSet(ProblemSet problemSet) {
        // measure 문제 테이블셋 처리
        return measureString(problemSet.getProblemSetId())
                + measureString(problemSet.getDdl())
                + measureString(problemSet.getData());
    }

    private long measureProblemSolveHistory(ProblemSolveHistory history) {
        // measure 문제 Solve 기록 처리
        return measureString(history.getProblemId())
                + measureString(history.getHandle())
                + measureString(resolveDbmsType(history).name())
                + measureString(history.getSubmittedSql())
                + Long.BYTES
                + Double.BYTES
                + Long.BYTES
                + Long.BYTES
                + measureString(history.getSubmittedAt().toString());
    }

    private long measureString(String value) {
        // measure String 처리
        if (value == null) {
            return 0;
        }

        return value.getBytes(StandardCharsets.UTF_8).length;
    }

    private record ProblemSubmittedHistoryKey(String problemId, String handle) {
    }

    private record ProblemSubmissionStats(int totalSubmitCount, int successSubmitCount) {

        private static ProblemSubmissionStats empty() {
            // empty 처리
            return new ProblemSubmissionStats(0, 0);
        }

        private static ProblemSubmissionStats from(boolean success) {
            // from 처리
            return new ProblemSubmissionStats(1, success ? 1 : 0);
        }

        private ProblemSubmissionStats merge(ProblemSubmissionStats other) {
            // merge 처리
            return new ProblemSubmissionStats(
                    totalSubmitCount + other.totalSubmitCount,
                    successSubmitCount + other.successSubmitCount
            );
        }
    }

    public record ProblemListEntry(Problem problem,
                                   List<ProblemSolveHistory> submittedHistories,
                                   int solvedUserCount,
                                   boolean solvedByCurrentUser,
                                   int totalSubmitCount,
                                   int successSubmitCount,
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
