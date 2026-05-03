package com.quertimizer.dashboard.adapter.in.web.response;

import com.quertimizer.dashboard.application.output.DashboardCommunityPostOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class DashboardCommunityPostRes {

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

    public static DashboardCommunityPostRes from(DashboardCommunityPostOutput result) {
        return new DashboardCommunityPostRes(
                result.postId(),
                result.title(),
                result.authorHandle(),
                result.excerpt(),
                result.tags(),
                result.category(),
                result.createdAt(),
                result.viewCount(),
                result.likeCount(),
                result.commentCount(),
                result.hotScore()
        );
    }

}
