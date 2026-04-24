package com.quertimizer.ranking.application.output;

public record RankListItemOutput(String handle,
                                 int solvedCount,
                                 double avgExecutionPercentile,
                                 RankMonthlyDeltaOutput monthlyRankDelta) {
}
