package com.quertimizer.dashboard.domain.policy;

import com.quertimizer.problem.application.output.ProblemListEntry;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Objects;

import static com.quertimizer.dashboard.domain.model.DashboardProblemRecommendationConstant.CANDIDATE_LIMIT_PER_DBMS;
import static com.quertimizer.dashboard.domain.model.DashboardProblemRecommendationConstant.DISPLAY_LIMIT;
import static com.quertimizer.dashboard.domain.model.DashboardProblemRecommendationConstant.SOLVED_USER_WEIGHT;
import static com.quertimizer.dashboard.domain.model.DashboardProblemRecommendationConstant.SUCCESS_SUBMIT_WEIGHT;
import static com.quertimizer.dashboard.domain.model.DashboardProblemRecommendationConstant.TOTAL_SUBMIT_WEIGHT;

public class DashboardProblemRecommendationPolicy {
    public int getDisplayLimit() {
        return DISPLAY_LIMIT;
    }

    public int getCandidateLimitPerDbms() {
        return CANDIDATE_LIMIT_PER_DBMS;
    }

    public Comparator<ProblemListEntry> createPopularityComparator() {
        return Comparator.comparingLong(this::calculatePopularityScore)
                .reversed()
                .thenComparing(problemEntry -> problemEntry.getProblem().getProblemId());
    }

    public Comparator<ProblemListEntry> createDailyShuffleComparator(LocalDate basisDate) {
        return Comparator.<ProblemListEntry>comparingLong(problemEntry -> calculateDailyShuffleKey(problemEntry, basisDate))
                .thenComparing(createPopularityComparator());
    }

    public long calculatePopularityScore(ProblemListEntry problemEntry) {
        return (long) problemEntry.getSolvedUserCount() * SOLVED_USER_WEIGHT
                + (long) problemEntry.getTotalSubmitCount() * TOTAL_SUBMIT_WEIGHT
                + (long) problemEntry.getSuccessSubmitCount() * SUCCESS_SUBMIT_WEIGHT;
    }

    private long calculateDailyShuffleKey(ProblemListEntry problemEntry, LocalDate basisDate) {
        // 일일 셔플 키 계산
        return Integer.toUnsignedLong(Objects.hash(problemEntry.getProblem().getProblemId(), basisDate));
    }
}
