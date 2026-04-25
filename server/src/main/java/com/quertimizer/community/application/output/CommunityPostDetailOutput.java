package com.quertimizer.community.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class CommunityPostDetailOutput {

    private final String postId;
    private final String title;
    private final String authorId;
    private final String contentJson;
    private final List<String> imageIds;
    private final List<String> tags;
    private final String category;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final int viewCount;
    private final int likeCount;
    private final int commentCount;
    private final boolean likedByCurrentUser;
    private final boolean editable;
    private final List<CommunityCommentOutput> comments;
}
