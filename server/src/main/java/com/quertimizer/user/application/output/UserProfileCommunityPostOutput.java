package com.quertimizer.user.application.output;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class UserProfileCommunityPostOutput {

    private final String postId;
    private final String title;
    private final String excerpt;
    private final List<String> tags;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final int likeCount;
    private final int commentCount;
}
