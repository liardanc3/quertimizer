package com.quertimizer.user.domain.model;

import lombok.Data;

@Data
public class UserProfileProblemSummary {

    private final int solvedProblemCount;
    private final long solvedExecutionTimeSumMs;
    private final Double postgresqlExecutionPercentile;
    private final Double mysqlExecutionPercentile;

}
