package com.quertimizer.community.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class CommunityCommentOutput {

    private final Long commentId;
    private final String authorId;
    private final String content;
    private final LocalDateTime createdAt;
    private final int likeCount;
    private final boolean likedByCurrentUser;
    private final List<CommunityCommentOutput> replies;
}
