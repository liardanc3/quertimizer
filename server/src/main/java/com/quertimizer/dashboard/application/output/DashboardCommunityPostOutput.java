package com.quertimizer.dashboard.application.output;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardCommunityPostOutput(String postId,
                                           String title,
                                           String authorHandle,
                                           String excerpt,
                                           List<String> tags,
                                           String category,
                                           LocalDateTime createdAt,
                                           int viewCount,
                                           int likeCount,
                                           int commentCount,
                                           double hotScore) {
}
