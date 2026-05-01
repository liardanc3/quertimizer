package com.quertimizer.dashboard.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class DashboardCommunityPostOutput {

    private final String postId;
    private final String title;
    private final String authorHandle;
    private final String excerpt;
    private final List<String> tags;
    private final String category;
    private final LocalDateTime createdAt;
    private final int viewCount;
    private final int likeCount;
    private final int commentCount;
    private final double hotScore;
}
