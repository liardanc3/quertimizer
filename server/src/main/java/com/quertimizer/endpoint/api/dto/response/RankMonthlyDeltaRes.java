package com.quertimizer.endpoint.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RankMonthlyDeltaRes {

    private final int solvedCount;
    private final int avgExecutionPercentile;

}
