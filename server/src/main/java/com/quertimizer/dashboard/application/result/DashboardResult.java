package com.quertimizer.dashboard.application.result;

import java.util.List;

public record DashboardResult(boolean authenticated,
                              String currentHandle,
                              List<DashboardCommunityPostResult> communityPosts,
                              List<DashboardProblemRecommendationResult> problems) {
}
