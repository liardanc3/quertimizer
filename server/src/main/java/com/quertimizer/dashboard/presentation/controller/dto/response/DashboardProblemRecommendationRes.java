package com.quertimizer.dashboard.presentation.controller.dto.response;

import com.quertimizer.dashboard.application.output.DashboardProblemRecommendationOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class DashboardProblemRecommendationRes {

    private final String problemId;
    private final String title;
    private final String dbms;
    private final int solvedUserCount;
    private final int totalSubmitCount;
    private final int successSubmitCount;
    private final double spreadRate;
    private final boolean solvedByCurrentUser;

    public static DashboardProblemRecommendationRes from(DashboardProblemRecommendationOutput result) {
        return new DashboardProblemRecommendationRes(
                result.problemId(),
                result.title(),
                result.dbms(),
                result.solvedUserCount(),
                result.totalSubmitCount(),
                result.successSubmitCount(),
                result.spreadRate(),
                result.solvedByCurrentUser()
        );
    }

}
