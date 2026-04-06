package com.quertimizer.endpoint.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RankListItemRes {

    private final String userId;
    private final int solvedCount;
    private final double avgExecutionPercentile;
    private final RankMonthlyDeltaRes monthlyRankDelta;

}
