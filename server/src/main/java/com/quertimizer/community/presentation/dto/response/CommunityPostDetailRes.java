package com.quertimizer.community.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class CommunityPostDetailRes {

    private final String postId;
    private final String title;
    private final String authorId;
    private final String contentHtml;
    private final List<String> tags;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final int viewCount;
    private final int likeCount;
    private final int commentCount;
    private final boolean likedByCurrentUser;
    private final boolean editable;
    private final List<CommunityCommentRes> comments;

}
