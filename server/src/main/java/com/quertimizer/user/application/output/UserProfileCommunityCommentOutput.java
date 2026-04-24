package com.quertimizer.user.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class UserProfileCommunityCommentOutput {

    private final Long commentId;
    private final String postId;
    private final String postTitle;
    private final String content;
    private final LocalDateTime createdAt;
    private final boolean reply;
}
