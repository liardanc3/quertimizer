package com.quertimizer.dashboard.application.output;

import java.util.List;

public record DashboardOutput(boolean authenticated,
                              String currentHandle,
                              List<DashboardCommunityPostOutput> communityPosts,
                              List<DashboardProblemRecommendationOutput> problems) {
}
