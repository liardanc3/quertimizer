package com.quertimizer.user.application.port.out;

import com.quertimizer.user.application.output.UserProfileCommunityActivitiesOutput;
import com.quertimizer.user.application.output.UserProfileCommunityCommentsOutput;
import com.quertimizer.user.application.output.UserProfileCommunityPostsOutput;
import com.quertimizer.user.domain.model.UserProfileCommunityCounts;

public interface UserProfileCommunityPort {

    UserProfileCommunityCounts getCommunityCounts(String handle);

    UserProfileCommunityPostsOutput getAuthoredPosts(String handle);

    UserProfileCommunityPostsOutput getLikedPosts(String handle);

    UserProfileCommunityCommentsOutput getAuthoredComments(String handle);

    UserProfileCommunityCommentsOutput getLikedComments(String handle);

    UserProfileCommunityActivitiesOutput getActivities(String handle, int requestedPage, Integer requestedPageSize);

}
