package com.quertimizer.ranking.adapter.in.http.response;

import com.quertimizer.ranking.application.output.RankListItemOutput;
import lombok.Data;

@Data
public class RankListItemRes {

    private final int rank;
    private final String handle;
    private final int solvedCount;
    private final double avgExecutionPercentile;
    private final int totalSubmitCount;
    private final int successSubmitCount;
    private final RankMonthlyDeltaRes monthlyRankDelta;

    public static RankListItemRes from(RankListItemOutput result) {
        return new RankListItemRes(
                result.rank(),
                result.handle(),
                result.solvedCount(),
                result.avgExecutionPercentile(),
                result.totalSubmitCount(),
                result.successSubmitCount(),
                RankMonthlyDeltaRes.from(result.monthlyRankDelta())
        );
    }

}
