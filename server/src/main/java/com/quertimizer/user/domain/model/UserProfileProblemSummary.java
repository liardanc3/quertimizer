package com.quertimizer.user.domain.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class UserProfileProblemSummary {

    private final int solvedProblemCount;
    private final long solvedExecutionTimeSumMs;
    private final Double postgresqlExecutionPercentile;
    private final Double mysqlExecutionPercentile;

}
