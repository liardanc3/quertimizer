package com.quertimizer.dashboard.domain.policy;

import com.quertimizer.community.domain.entity.CommunityPost;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;

import static com.quertimizer.dashboard.domain.model.DashboardHotPostConstant.COMMENT_WEIGHT;
import static com.quertimizer.dashboard.domain.model.DashboardHotPostConstant.DISPLAY_LIMIT;
import static com.quertimizer.dashboard.domain.model.DashboardHotPostConstant.LIKE_WEIGHT;
import static com.quertimizer.dashboard.domain.model.DashboardHotPostConstant.RECENCY_BONUS_HOURS;
import static com.quertimizer.dashboard.domain.model.DashboardHotPostConstant.RECENCY_WEIGHT;
import static com.quertimizer.dashboard.domain.model.DashboardHotPostConstant.VIEW_WEIGHT;

public class DashboardHotPostPolicy {
    public int getDisplayLimit() {
        return DISPLAY_LIMIT;
    }

    public Comparator<CommunityPost> createHotPostComparator() {
        return Comparator.comparingDouble(this::calculateHotScore)
                .reversed()
                .thenComparing(CommunityPost::getCreatedAt, Comparator.reverseOrder())
                .thenComparing(CommunityPost::getPostId);
    }

    public double calculateHotScore(CommunityPost post) {
        long elapsedHours = Math.max(0L, Duration.between(post.getCreatedAt(), LocalDateTime.now()).toHours());
        double recencyBonus = Math.max(0L, RECENCY_BONUS_HOURS - elapsedHours) * RECENCY_WEIGHT;

        return post.getLikeCount() * LIKE_WEIGHT
                + post.getCommentCount() * COMMENT_WEIGHT
                + Math.sqrt(Math.max(0, post.getViewCount())) * VIEW_WEIGHT
                + recencyBonus;
    }
}
