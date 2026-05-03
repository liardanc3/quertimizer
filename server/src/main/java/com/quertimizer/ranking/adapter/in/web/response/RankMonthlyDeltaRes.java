package com.quertimizer.ranking.adapter.in.web.response;

import com.quertimizer.ranking.application.output.RankMonthlyDeltaOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RankMonthlyDeltaRes {

    private final int solvedCount;
    private final int avgExecutionPercentile;

    public static RankMonthlyDeltaRes from(RankMonthlyDeltaOutput result) {
        return new RankMonthlyDeltaRes(result.solvedCount(), result.avgExecutionPercentile());
    }

}
