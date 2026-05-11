package com.quertimizer.user.application.output;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserProfileCommunityCommentOutput {

    private final Long commentId;
    private final String postId;
    private final String postTitle;
    private final String content;
    private final LocalDateTime createdAt;
    private final boolean reply;
}
