package com.quertimizer.dashboard.application.result;

import java.time.LocalDateTime;
import java.util.List;

public record DashboardCommunityPostResult(String postId,
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
