package com.quertimizer.community.presentation.controller.dto.response;

import com.quertimizer.community.application.output.CommunityCommentOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class CommunityCommentRes {

    private final Long commentId;
    private final String authorId;
    private final String content;
    private final LocalDateTime createdAt;
    private final int likeCount;
    private final boolean likedByCurrentUser;
    private final List<CommunityCommentRes> replies;

    public static CommunityCommentRes from(CommunityCommentOutput result) {
        return new CommunityCommentRes(
                result.getCommentId(),
                result.getAuthorId(),
                result.getContent(),
                result.getCreatedAt(),
                result.getLikeCount(),
                result.isLikedByCurrentUser(),
                result.getReplies().stream()
                        .map(CommunityCommentRes::from)
                        .toList()
        );
    }
}
