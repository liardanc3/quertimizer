package com.quertimizer.endpoint.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class CommunityPostSummaryRes {

    private final String postId;
    private final String title;
    private final String authorId;
    private final String excerpt;
    private final List<String> tags;
    private final String category;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final int viewCount;
    private final int likeCount;
    private final int commentCount;

}
