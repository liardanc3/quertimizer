package com.quertimizer.problem.application.service;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.output.ProblemListEntry;
import com.quertimizer.problem.application.output.ProblemPage;
import com.quertimizer.problem.application.port.out.ProblemRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemSetRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemSolveHistoryRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemSubmitHistoryRepositoryPort;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.domain.entity.ProblemSet;
import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import com.quertimizer.problem.domain.entity.ProblemSubmitHistory;
import com.quertimizer.problem.domain.model.ProblemPageConstant;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Accessors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProblemSearchService {

    private final ProblemRepositoryPort problemRepository;
    private final ProblemSetRepositoryPort problemSetRepository;
    private final ProblemSolveHistoryRepositoryPort problemSolveHistoryRepository;
    private final ProblemSubmitHistoryRepositoryPort problemSubmitHistoryRepository;

    public ProblemPage findProblemPage(int requestedPage, String searchKeyword, DbmsType dbmsType,
                                       String solveState, String currentHandle,
                                       String problemIdSort, String solvedCountSort,
                                       String totalSubmitSort, String successSubmitSort) {
        // DB에서 문제와 제출 통계 원천 데이터 직접 조회
        Map<String, List<ProblemSolveHistory>> bestHistoriesByProblemId =
                createBestSubmittedHistoriesByProblemId(problemSolveHistoryRepository.findAll());
        Map<String, ProblemSubmissionStats> submissionStatsByProblemId =
                createSubmissionStatsByProblemId(problemSubmitHistoryRepository.findAll());

        // 목록 필터와 정렬에 필요한 문제 항목 구성
        List<ProblemListEntry> searchableProblems = problemRepository.findAll().stream()
                .filter(Problem::hasSupportedDbms)
                .filter(problem -> problem.supportsDbms(dbmsType))
                .sorted(Comparator.comparing(Problem::getProblemId))
                .map(problem -> createProblemListEntry(problem, currentHandle, bestHistoriesByProblemId, submissionStatsByProblemId))
                .filter(problemEntry -> matchesSearch(problemEntry, searchKeyword))
                .filter(problemEntry -> matchesSolveState(problemEntry, solveState, currentHandle))
                .sorted(createProblemComparator(problemIdSort, solvedCountSort, totalSubmitSort, successSubmitSort))
                .toList();

        // 페이지 경계 계산
        int totalCount = searchableProblems.size();
        int totalPages = Math.max(1, (int) Math.ceil(totalCount / (double) ProblemPageConstant.PAGE_SIZE));
        int currentPage = Math.min(Math.max(requestedPage, 1), totalPages);
        int fromIndex = Math.min((currentPage - 1) * ProblemPageConstant.PAGE_SIZE, totalCount);
        int toIndex = Math.min(fromIndex + ProblemPageConstant.PAGE_SIZE, totalCount);

        // 현재 페이지 데이터 반환
        return new ProblemPage(
                currentPage, ProblemPageConstant.PAGE_SIZE,
                totalCount, totalPages, searchableProblems.subList(fromIndex, toIndex)
        );
    }

    public List<ProblemSet> findAllProblemSets() {
        // DB에서 지원 DBMS 테이블셋 목록 직접 조회
        return problemSetRepository.findAll().stream()
                .filter(ProblemSet::hasSupportedDbms)
                .sorted(Comparator.comparing(ProblemSet::getProblemSetId))
                .toList();
    }

    public List<ProblemSolveHistory> findBestSubmittedHistories(String problemId) {
        // DB에서 문제별 최고 제출 기록 직접 조회
        return createBestSubmittedHistoriesByProblemId(problemSolveHistoryRepository.findAllByProblemId(problemId))
                .getOrDefault(problemId, List.of());
    }

    private ProblemListEntry createProblemListEntry(Problem problem, String currentHandle,
                                                    Map<String, List<ProblemSolveHistory>> bestHistoriesByProblemId,
                                                    Map<String, ProblemSubmissionStats> submissionStatsByProblemId) {
        // 문제별 최고 제출 기록과 제출 통계 조회
        List<ProblemSolveHistory> submittedHistories = bestHistoriesByProblemId.getOrDefault(problem.getProblemId(), List.of());
        ProblemSubmissionStats submissionStats =
                submissionStatsByProblemId.getOrDefault(problem.getProblemId(), ProblemSubmissionStats.empty());

        // 해결 사용자 수와 현재 사용자 해결 여부 계산
        int solvedUserCount = (int) submittedHistories.stream()
                .map(ProblemSolveHistory::getHandle)
                .distinct()
                .count();
        boolean solvedByCurrentUser = currentHandle != null
                && submittedHistories.stream().anyMatch(history -> history.getHandle().equals(currentHandle));

        // 문제 목록 항목 반환
        return new ProblemListEntry(
                problem, submittedHistories, solvedUserCount, solvedByCurrentUser,
                submissionStats.getTotalSubmitCount(), submissionStats.getSuccessSubmitCount()
        );
    }

    private boolean matchesSearch(ProblemListEntry problemEntry, String searchKeyword) {
        // 검색어가 없으면 통과
        if (searchKeyword == null || searchKeyword.isBlank()) {
            return true;
        }

        // 문제 번호 또는 제목 검색 일치 여부 반환
        String normalizedSearchKeyword = searchKeyword.trim().toLowerCase();
        return problemEntry.getProblem().getProblemId().toLowerCase().contains(normalizedSearchKeyword)
                || problemEntry.getProblem().getTitle().toLowerCase().contains(normalizedSearchKeyword);
    }

    private boolean matchesSolveState(ProblemListEntry problemEntry, String solveState, String currentHandle) {
        // 풀이 상태 조건이 없으면 통과
        if (currentHandle == null || solveState == null || solveState.isBlank() || "all".equalsIgnoreCase(solveState)) {
            return true;
        }

        // 풀이 상태 없음 조건은 목록 비노출
        if ("none".equalsIgnoreCase(solveState)) {
            return false;
        }

        // 해결 여부 조건별 일치 여부 반환
        if ("solved".equalsIgnoreCase(solveState)) {
            return problemEntry.isSolvedByCurrentUser();
        }

        if ("unsolved".equalsIgnoreCase(solveState)) {
            return !problemEntry.isSolvedByCurrentUser();
        }

        return true;
    }

    private Comparator<ProblemListEntry> createProblemComparator(String problemIdSort,
                                                                String solvedCountSort,
                                                                String totalSubmitSort,
                                                                String successSubmitSort) {
        // 문제 번호 보조 정렬 기준 준비
        Comparator<ProblemListEntry> problemIdComparator =
                Comparator.comparing(problemEntry -> problemEntry.getProblem().getProblemId());

        // 요청 정렬 조건별 comparator 반환
        if ("asc".equalsIgnoreCase(problemIdSort)) {
            return problemIdComparator;
        }

        if ("desc".equalsIgnoreCase(problemIdSort)) {
            return problemIdComparator.reversed();
        }

        if ("asc".equalsIgnoreCase(totalSubmitSort)) {
            return Comparator.comparingInt(ProblemListEntry::getTotalSubmitCount).thenComparing(problemIdComparator);
        }

        if ("desc".equalsIgnoreCase(totalSubmitSort)) {
            return Comparator.comparingInt(ProblemListEntry::getTotalSubmitCount).reversed().thenComparing(problemIdComparator);
        }

        if ("asc".equalsIgnoreCase(successSubmitSort)) {
            return Comparator.comparingInt(ProblemListEntry::getSuccessSubmitCount).thenComparing(problemIdComparator);
        }

        if ("desc".equalsIgnoreCase(successSubmitSort)) {
            return Comparator.comparingInt(ProblemListEntry::getSuccessSubmitCount).reversed().thenComparing(problemIdComparator);
        }

        if ("asc".equalsIgnoreCase(solvedCountSort)) {
            return Comparator.comparingInt(ProblemListEntry::getSolvedUserCount).thenComparing(problemIdComparator);
        }

        return Comparator.comparingInt(ProblemListEntry::getSolvedUserCount).reversed().thenComparing(problemIdComparator);
    }

    private Map<String, List<ProblemSolveHistory>> createBestSubmittedHistoriesByProblemId(List<ProblemSolveHistory> histories) {
        // 문제와 사용자 기준 최고 제출 기록 구성
        Map<ProblemSubmittedHistoryKey, ProblemSolveHistory> bestHistoryByKey = new HashMap<>();
        for (ProblemSolveHistory history : histories) {
            ProblemSubmittedHistoryKey historyKey = new ProblemSubmittedHistoryKey(history.getProblemId(), history.getHandle());
            bestHistoryByKey.merge(historyKey, history, this::pickBetterHistory);
        }

        // 문제 번호별 최고 제출 기록 목록 구성
        Map<String, List<ProblemSolveHistory>> submittedHistoriesByProblemId = new HashMap<>();
        for (ProblemSolveHistory history : bestHistoryByKey.values()) {
            submittedHistoriesByProblemId.computeIfAbsent(history.getProblemId(), key -> new ArrayList<>()).add(history);
        }

        // 문제별 응답 정렬 순서 적용
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
        // 비용이 더 낮은 제출 우선 선택
        if (candidateHistory.getCost() < currentHistory.getCost()) {
            return candidateHistory;
        }

        if (candidateHistory.getCost() > currentHistory.getCost()) {
            return currentHistory;
        }

        // 비용이 같으면 실행 시간이 더 짧은 제출 선택
        if (candidateHistory.getExecutionTimeMs() < currentHistory.getExecutionTimeMs()) {
            return candidateHistory;
        }

        if (candidateHistory.getExecutionTimeMs() > currentHistory.getExecutionTimeMs()) {
            return currentHistory;
        }

        // 비용과 실행 시간이 같으면 더 먼저 제출한 기록 선택
        if (candidateHistory.getSubmittedAt().isBefore(currentHistory.getSubmittedAt())) {
            return candidateHistory;
        }

        return currentHistory;
    }

    private Map<String, ProblemSubmissionStats> createSubmissionStatsByProblemId(List<ProblemSubmitHistory> histories) {
        // 문제 번호별 제출 통계 생성
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

    @Getter
    @EqualsAndHashCode
    @Accessors(fluent = true)
    @RequiredArgsConstructor
    private static class ProblemSubmittedHistoryKey {
        private final String problemId;
        private final String handle;
    }

    @Getter
    @RequiredArgsConstructor
    private static class ProblemSubmissionStats {
        private final int totalSubmitCount;
        private final int successSubmitCount;

        private static ProblemSubmissionStats empty() {
            // 빈 제출 통계 생성
            return new ProblemSubmissionStats(0, 0);
        }

        private static ProblemSubmissionStats from(boolean success) {
            // 제출 성공 여부 기준 통계 생성
            return new ProblemSubmissionStats(1, success ? 1 : 0);
        }

        private ProblemSubmissionStats merge(ProblemSubmissionStats other) {
            // 제출 통계 합산
            return new ProblemSubmissionStats(
                    totalSubmitCount + other.totalSubmitCount,
                    successSubmitCount + other.successSubmitCount
            );
        }
    }
}
