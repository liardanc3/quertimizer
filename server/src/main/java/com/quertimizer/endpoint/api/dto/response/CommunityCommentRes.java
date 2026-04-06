package com.quertimizer.endpoint.api.dto.response;

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

}
