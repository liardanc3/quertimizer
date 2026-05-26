package com.quertimizer.community.application.output;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class CommunityDashboardPostCandidateOutput {

    private final String postId;
    private final String title;
    private final String handle;
    private final String contentJson;
    private final String plainTextSummary;
    private final List<String> tags;
    private final String category;
    private final LocalDateTime createdAt;
    private final int viewCount;
    private final int likeCount;
    private final int commentCount;
}
