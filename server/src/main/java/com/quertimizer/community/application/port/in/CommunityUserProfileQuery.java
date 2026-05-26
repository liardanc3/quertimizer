package com.quertimizer.community.application.port.in;

import com.quertimizer.community.application.output.CommunityUserActivitiesOutput;
import com.quertimizer.community.application.output.CommunityUserCommentOutput;
import com.quertimizer.community.application.output.CommunityUserCountsOutput;
import com.quertimizer.community.application.output.CommunityUserPostOutput;

import java.util.List;

public interface CommunityUserProfileQuery {

    CommunityUserCountsOutput getCounts(String handle);

    List<CommunityUserPostOutput> getAuthoredPosts(String handle);

    List<CommunityUserPostOutput> getLikedPosts(String handle);

    List<CommunityUserCommentOutput> getAuthoredComments(String handle);

    List<CommunityUserCommentOutput> getLikedComments(String handle);

    CommunityUserActivitiesOutput getActivities(String handle, int requestedPage, Integer requestedPageSize);
}
