package com.quertimizer.dashboard.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class DashboardProblemRecommendationOutput {

    private final String problemId;
    private final String title;
    private final String dbms;
    private final int solvedUserCount;
    private final int totalSubmitCount;
    private final int successSubmitCount;
    private final double spreadRate;
    private final boolean solvedByCurrentUser;
}
