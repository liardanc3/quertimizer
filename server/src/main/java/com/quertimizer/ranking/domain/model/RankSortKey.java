package com.quertimizer.ranking.domain.model;

public enum RankSortKey {
    SOLVED_COUNT("solvedCount"),
    AVG_EXECUTION_PERCENTILE("avgExecutionPercentile");

    private final String value;

    RankSortKey(String value) {
        this.value = value;
    }

    public static RankSortKey fromValueOrDefault(String value) {
        // 실행 계획 백분위 정렬 외 기존 기본 정렬 유지
        if (AVG_EXECUTION_PERCENTILE.value.equalsIgnoreCase(value)) {
            return AVG_EXECUTION_PERCENTILE;
        }

        return SOLVED_COUNT;
    }
}
