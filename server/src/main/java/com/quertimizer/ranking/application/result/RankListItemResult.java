package com.quertimizer.ranking.application.result;

public record RankListItemResult(String handle,
                                 int solvedCount,
                                 double avgExecutionPercentile,
                                 RankMonthlyDeltaResult monthlyRankDelta) {
}
