package com.quertimizer.user.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class UserProfileCommunityPostRes {

    private final String postId;
    private final String title;
    private final String excerpt;
    private final List<String> tags;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;
    private final int likeCount;
    private final int commentCount;

}
