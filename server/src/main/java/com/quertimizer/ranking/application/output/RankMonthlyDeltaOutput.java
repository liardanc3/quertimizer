package com.quertimizer.ranking.application.output;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(fluent = true)
public class RankMonthlyDeltaOutput {

    private final int solvedCount;
    private final int avgExecutionPercentile;
}
