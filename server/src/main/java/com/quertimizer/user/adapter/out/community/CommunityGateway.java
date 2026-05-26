package com.quertimizer.user.adapter.out.community;

import com.quertimizer.community.application.output.CommunityUserActivitiesOutput;
import com.quertimizer.community.application.output.CommunityUserActivityOutput;
import com.quertimizer.community.application.output.CommunityUserCommentOutput;
import com.quertimizer.community.application.output.CommunityUserCountsOutput;
import com.quertimizer.community.application.output.CommunityUserPostOutput;
import com.quertimizer.community.application.port.in.CommunityUserProfileQuery;
import com.quertimizer.user.application.output.UserProfileCommunityActivitiesOutput;
import com.quertimizer.user.application.output.UserProfileCommunityActivityOutput;
import com.quertimizer.user.application.output.UserProfileCommunityCommentOutput;
import com.quertimizer.user.application.output.UserProfileCommunityCommentsOutput;
import com.quertimizer.user.application.output.UserProfileCommunityPostOutput;
import com.quertimizer.user.application.output.UserProfileCommunityPostsOutput;
import com.quertimizer.user.application.port.out.UserProfileCommunityPort;
import com.quertimizer.user.domain.model.UserProfileCommunityCounts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("userCommunityGateway")
@RequiredArgsConstructor
public class CommunityGateway implements UserProfileCommunityPort {

    private final CommunityUserProfileQuery communityUserProfileQuery;

    @Override
    public UserProfileCommunityCounts getCommunityCounts(String handle) {
        // community 공개 query 기준 사용자 커뮤니티 활동 수 변환
        CommunityUserCountsOutput counts = communityUserProfileQuery.getCounts(handle);
        return new UserProfileCommunityCounts(
                counts.getAuthoredPostCount(),
                counts.getLikedPostCount(),
                counts.getAuthoredCommentCount()
        );
    }

    @Override
    public UserProfileCommunityPostsOutput getAuthoredPosts(String handle) {
        // community 공개 query 기준 작성 게시글 변환
        return new UserProfileCommunityPostsOutput(communityUserProfileQuery.getAuthoredPosts(handle).stream()
                .map(this::toPostOutput)
                .toList());
    }

    @Override
    public UserProfileCommunityPostsOutput getLikedPosts(String handle) {
        // community 공개 query 기준 좋아요 게시글 변환
        return new UserProfileCommunityPostsOutput(communityUserProfileQuery.getLikedPosts(handle).stream()
                .map(this::toPostOutput)
                .toList());
    }

    @Override
    public UserProfileCommunityCommentsOutput getAuthoredComments(String handle) {
        // community 공개 query 기준 작성 댓글 변환
        return new UserProfileCommunityCommentsOutput(communityUserProfileQuery.getAuthoredComments(handle).stream()
                .map(this::toCommentOutput)
                .toList());
    }

    @Override
    public UserProfileCommunityCommentsOutput getLikedComments(String handle) {
        // community 공개 query 기준 좋아요 댓글 변환
        return new UserProfileCommunityCommentsOutput(communityUserProfileQuery.getLikedComments(handle).stream()
                .map(this::toCommentOutput)
                .toList());
    }

    @Override
    public UserProfileCommunityActivitiesOutput getActivities(String handle, int requestedPage, Integer requestedPageSize) {
        // community 공개 query 기준 커뮤니티 활동 변환
        CommunityUserActivitiesOutput activities = communityUserProfileQuery.getActivities(handle, requestedPage, requestedPageSize);
        return new UserProfileCommunityActivitiesOutput(
                activities.getCurrentPage(), activities.getPageSize(), activities.getTotalCount(), activities.getTotalPages(),
                activities.getActivities().stream().map(this::toActivityOutput).toList()
        );
    }

    private UserProfileCommunityPostOutput toPostOutput(CommunityUserPostOutput output) {
        // community 게시글 응답을 user 프로필 응답으로 변환
        return new UserProfileCommunityPostOutput(
                output.getPostId(), output.getTitle(), output.getExcerpt(), output.getTags(),
                output.getCreatedAt(), output.getUpdatedAt(), output.getLikeCount(), output.getCommentCount()
        );
    }

    private UserProfileCommunityCommentOutput toCommentOutput(CommunityUserCommentOutput output) {
        // community 댓글 응답을 user 프로필 응답으로 변환
        return new UserProfileCommunityCommentOutput(
                output.getCommentId(), output.getPostId(), output.getPostTitle(), output.getContent(),
                output.getActedAt(), output.isChildComment()
        );
    }

    private UserProfileCommunityActivityOutput toActivityOutput(CommunityUserActivityOutput output) {
        // community 활동 응답을 user 프로필 응답으로 변환
        return new UserProfileCommunityActivityOutput(
                output.getActivityType(), output.getPostId(), output.getTitle(), output.getCommentId(),
                output.getExcerpt(), output.getHappenedAt()
        );
    }
}
