package com.quertimizer.dashboard.domain.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class DashboardCommunityPostCandidate {

    private final String postId;
    private final String title;
    private final String authorHandle;
    private final String plainTextSummary;
    private final List<String> tags;
    private final String category;
    private final LocalDateTime createdAt;
    private final int viewCount;
    private final int likeCount;
    private final int commentCount;

}
