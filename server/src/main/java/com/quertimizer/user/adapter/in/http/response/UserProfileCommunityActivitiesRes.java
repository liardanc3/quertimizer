package com.quertimizer.user.adapter.in.http.response;

import com.quertimizer.user.application.output.UserProfileCommunityActivitiesOutput;
import lombok.Data;

import java.util.List;

@Data
public class UserProfileCommunityActivitiesRes {

    private final int currentPage;
    private final int pageSize;
    private final long totalCount;
    private final int totalPages;
    private final List<UserProfileCommunityActivityRes> activities;

    public static UserProfileCommunityActivitiesRes from(UserProfileCommunityActivitiesOutput result) {
        return new UserProfileCommunityActivitiesRes(
                result.getCurrentPage(),
                result.getPageSize(),
                result.getTotalCount(),
                result.getTotalPages(),
                result.getActivities().stream()
                        .map(UserProfileCommunityActivityRes::from)
                        .toList()
        );
    }
}
