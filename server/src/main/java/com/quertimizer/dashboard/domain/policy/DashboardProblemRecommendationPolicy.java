package com.quertimizer.dashboard.domain.policy;

import com.quertimizer.problem.application.store.ProblemStore;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;

@Component
public class DashboardProblemRecommendationPolicy {

    private static final int DISPLAY_LIMIT = 12;
    private static final int CANDIDATE_LIMIT_PER_DBMS = 16;
    private static final int SOLVED_USER_WEIGHT = 5;
    private static final int TOTAL_SUBMIT_WEIGHT = 2;
    private static final int SUCCESS_SUBMIT_WEIGHT = 3;

    public int getDisplayLimit() {
        // 표시 개수 조회
        return DISPLAY_LIMIT;
    }

    public int getCandidateLimitPerDbms() {
        // DBMS별 후보 개수 조회
        return CANDIDATE_LIMIT_PER_DBMS;
    }

    public Comparator<ProblemStore.ProblemListEntry> createPopularityComparator() {
        // 인기도 비교 기준 생성
        return Comparator.comparingLong(this::calculatePopularityScore)
                .reversed()
                .thenComparing(problemEntry -> problemEntry.problem().getProblemId());
    }

    public Comparator<ProblemStore.ProblemListEntry> createDailyShuffleComparator(LocalDate basisDate) {
        // 일일 셔플 비교 기준 생성
        return Comparator.<ProblemStore.ProblemListEntry>comparingLong(problemEntry -> calculateDailyShuffleKey(problemEntry, basisDate))
                .thenComparing(createPopularityComparator());
    }

    public long calculatePopularityScore(ProblemStore.ProblemListEntry problemEntry) {
        // 인기도 점수 계산
        return (long) problemEntry.solvedUserCount() * SOLVED_USER_WEIGHT
                + (long) problemEntry.totalSubmitCount() * TOTAL_SUBMIT_WEIGHT
                + (long) problemEntry.successSubmitCount() * SUCCESS_SUBMIT_WEIGHT;
    }

    private long calculateDailyShuffleKey(ProblemStore.ProblemListEntry problemEntry, LocalDate basisDate) {
        // 일일 셔플 키 계산
        return Integer.toUnsignedLong(Objects.hash(problemEntry.problem().getProblemId(), basisDate));
    }

}
