package com.quertimizer.dashboard.application.output;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(fluent = true)
public class DashboardOutput {

    private final boolean authenticated;
    private final String currentHandle;
    private final List<DashboardCommunityPostOutput> communityPosts;
    private final List<DashboardProblemRecommendationOutput> problems;
}
