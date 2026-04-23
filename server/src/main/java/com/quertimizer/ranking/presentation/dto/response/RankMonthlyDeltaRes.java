package com.quertimizer.ranking.presentation.dto.response;

import com.quertimizer.ranking.application.result.RankMonthlyDeltaResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RankMonthlyDeltaRes {

    private final int solvedCount;
    private final int avgExecutionPercentile;

    public static RankMonthlyDeltaRes from(RankMonthlyDeltaResult result) {
        return new RankMonthlyDeltaRes(result.solvedCount(), result.avgExecutionPercentile());
    }

}
