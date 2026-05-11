package com.quertimizer.ranking.application.output;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class RankListItemOutput {

    private final String handle;
    private final int solvedCount;
    private final double avgExecutionPercentile;
    private final int totalSubmitCount;
    private final int successSubmitCount;
    private final RankMonthlyDeltaOutput monthlyRankDelta;
}
