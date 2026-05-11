package com.quertimizer.user.adapter.in.web.response;

import com.quertimizer.user.application.output.UserProfileCommunityActivityOutput;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileCommunityActivityRes {

    private final String activityType;
    private final String postId;
    private final String postTitle;
    private final Long commentId;
    private final String content;
    private final LocalDateTime happenedAt;

    public static UserProfileCommunityActivityRes from(UserProfileCommunityActivityOutput result) {
        return new UserProfileCommunityActivityRes(
                result.getActivityType(),
                result.getPostId(),
                result.getPostTitle(),
                result.getCommentId(),
                result.getContent(),
                result.getHappenedAt()
        );
    }
}
