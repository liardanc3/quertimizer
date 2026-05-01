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

    /**
     * 대시보드 추천 문제 표시 개수를 반환한다.
     *
     * @return 추천 문제 표시 개수
     */
    public int getDisplayLimit() {
        return DISPLAY_LIMIT;
    }

    /**
     * DBMS별 추천 후보 개수를 반환한다.
     *
     * @return DBMS별 추천 후보 개수
     */
    public int getCandidateLimitPerDbms() {
        return CANDIDATE_LIMIT_PER_DBMS;
    }

    /**
     * 문제 인기도 정렬 기준을 생성한다.
     *
     * @return 인기도 점수와 문제 번호 기준 비교자
     */
    public Comparator<ProblemStore.ProblemListEntry> createPopularityComparator() {
        return Comparator.comparingLong(this::calculatePopularityScore)
                .reversed()
                .thenComparing(problemEntry -> problemEntry.problem().getProblemId());
    }

    /**
     * 일 단위 셔플 정렬 기준을 생성한다.
     *
     * @param basisDate 셔플 기준 날짜
     * @return 일 단위 셔플 키와 인기도 기준 비교자
     */
    public Comparator<ProblemStore.ProblemListEntry> createDailyShuffleComparator(LocalDate basisDate) {
        return Comparator.<ProblemStore.ProblemListEntry>comparingLong(problemEntry -> calculateDailyShuffleKey(problemEntry, basisDate))
                .thenComparing(createPopularityComparator());
    }

    /**
     * 문제 추천 인기도 점수를 계산한다.
     *
     * @param problemEntry 인기도 점수를 계산할 문제 목록 항목
     * @return 해결 사용자 수와 제출 통계 기반 인기도 점수
     */
    public long calculatePopularityScore(ProblemStore.ProblemListEntry problemEntry) {
        return (long) problemEntry.solvedUserCount() * SOLVED_USER_WEIGHT
                + (long) problemEntry.totalSubmitCount() * TOTAL_SUBMIT_WEIGHT
                + (long) problemEntry.successSubmitCount() * SUCCESS_SUBMIT_WEIGHT;
    }

    private long calculateDailyShuffleKey(ProblemStore.ProblemListEntry problemEntry, LocalDate basisDate) {
        // 일일 셔플 키 계산
        return Integer.toUnsignedLong(Objects.hash(problemEntry.problem().getProblemId(), basisDate));
    }

}
