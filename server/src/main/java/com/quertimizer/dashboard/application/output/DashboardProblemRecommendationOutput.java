package com.quertimizer.dashboard.application.output;

public record DashboardProblemRecommendationOutput(String problemId,
                                                   String title,
                                                   String dbms,
                                                   int solvedUserCount,
                                                   int totalSubmitCount,
                                                   int successSubmitCount,
                                                   double spreadRate,
                                                   boolean solvedByCurrentUser) {
}
