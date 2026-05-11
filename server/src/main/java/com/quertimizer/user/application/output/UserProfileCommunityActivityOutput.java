package com.quertimizer.user.application.output;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileCommunityActivityOutput {

    private final String activityType;
    private final String postId;
    private final String postTitle;
    private final Long commentId;
    private final String content;
    private final LocalDateTime happenedAt;
}
