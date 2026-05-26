package com.quertimizer.community.application.output;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommunityUserActivityOutput {

    private final String activityType;
    private final String postId;
    private final String title;
    private final Long commentId;
    private final String excerpt;
    private final LocalDateTime happenedAt;
}
