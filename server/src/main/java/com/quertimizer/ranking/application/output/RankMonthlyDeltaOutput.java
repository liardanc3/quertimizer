package com.quertimizer.ranking.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class RankMonthlyDeltaOutput {

    private final int solvedCount;
    private final int avgExecutionPercentile;
}
