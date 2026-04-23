package com.quertimizer.dashboard.application.result;

public record DashboardProblemRecommendationResult(String problemId,
                                                   String title,
                                                   String dbms,
                                                   int solvedUserCount,
                                                   int totalSubmitCount,
                                                   int successSubmitCount,
                                                   double spreadRate,
                                                   boolean solvedByCurrentUser) {
}
