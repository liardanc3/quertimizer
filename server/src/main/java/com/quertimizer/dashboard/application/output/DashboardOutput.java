package com.quertimizer.dashboard.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class DashboardOutput {

    private final boolean authenticated;
    private final String currentHandle;
    private final List<DashboardCommunityPostOutput> communityPosts;
    private final List<DashboardProblemRecommendationOutput> problems;
}
