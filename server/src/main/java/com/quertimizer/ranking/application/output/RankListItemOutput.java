package com.quertimizer.ranking.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class RankListItemOutput {

    private final String handle;
    private final int solvedCount;
    private final double avgExecutionPercentile;
    private final int totalSubmitCount;
    private final int successSubmitCount;
    private final RankMonthlyDeltaOutput monthlyRankDelta;
}
