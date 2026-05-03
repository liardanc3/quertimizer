package com.quertimizer.dashboard.adapter.in.web.response;

import com.quertimizer.dashboard.application.output.DashboardOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class DashboardRes {

    private final boolean authenticated;
    private final String currentHandle;
    private final List<DashboardCommunityPostRes> communityPosts;
    private final List<DashboardProblemRecommendationRes> problems;

    public static DashboardRes from(DashboardOutput result) {
        return new DashboardRes(
                result.authenticated(),
                result.currentHandle(),
                result.communityPosts().stream()
                        .map(DashboardCommunityPostRes::from)
                        .toList(),
                result.problems().stream()
                        .map(DashboardProblemRecommendationRes::from)
                        .toList()
        );
    }

}
