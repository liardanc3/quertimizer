package com.quertimizer.community.application.output;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommunityUserCommentOutput {

    private final Long commentId;
    private final String postId;
    private final String postTitle;
    private final String content;
    private final LocalDateTime actedAt;
    private final boolean childComment;
}
