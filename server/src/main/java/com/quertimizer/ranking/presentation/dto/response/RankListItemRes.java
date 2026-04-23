package com.quertimizer.ranking.presentation.dto.response;

import com.quertimizer.ranking.application.result.RankListItemResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RankListItemRes {

    private final String handle;
    private final int solvedCount;
    private final double avgExecutionPercentile;
    private final RankMonthlyDeltaRes monthlyRankDelta;

    public static RankListItemRes from(RankListItemResult result) {
        return new RankListItemRes(
                result.handle(),
                result.solvedCount(),
                result.avgExecutionPercentile(),
                RankMonthlyDeltaRes.from(result.monthlyRankDelta())
        );
    }

}
