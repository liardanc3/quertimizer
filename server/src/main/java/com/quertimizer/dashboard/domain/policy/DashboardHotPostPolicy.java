package com.quertimizer.dashboard.domain.policy;

import com.quertimizer.dashboard.domain.model.DashboardCommunityPostCandidate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;

import static com.quertimizer.dashboard.domain.model.DashboardHotPostConstant.COMMENT_WEIGHT;
import static com.quertimizer.dashboard.domain.model.DashboardHotPostConstant.DISPLAY_LIMIT;
import static com.quertimizer.dashboard.domain.model.DashboardHotPostConstant.LIKE_WEIGHT;
import static com.quertimizer.dashboard.domain.model.DashboardHotPostConstant.RECENCY_BONUS_HOURS;
import static com.quertimizer.dashboard.domain.model.DashboardHotPostConstant.RECENCY_WEIGHT;
import static com.quertimizer.dashboard.domain.model.DashboardHotPostConstant.VIEW_WEIGHT;

@Component
public class DashboardHotPostPolicy {
    public int getDisplayLimit() {
        return DISPLAY_LIMIT;
    }

    public Comparator<DashboardCommunityPostCandidate> createHotPostComparator() {
        return Comparator.comparingDouble(this::calculateHotScore)
                .reversed()
                .thenComparing(DashboardCommunityPostCandidate::getCreatedAt, Comparator.reverseOrder())
                .thenComparing(DashboardCommunityPostCandidate::getPostId);
    }

    public double calculateHotScore(DashboardCommunityPostCandidate post) {
        long elapsedHours = Math.max(0L, Duration.between(post.getCreatedAt(), LocalDateTime.now()).toHours());
        double recencyBonus = Math.max(0L, RECENCY_BONUS_HOURS - elapsedHours) * RECENCY_WEIGHT;

        return post.getLikeCount() * LIKE_WEIGHT
                + post.getCommentCount() * COMMENT_WEIGHT
                + Math.sqrt(Math.max(0, post.getViewCount())) * VIEW_WEIGHT
                + recencyBonus;
    }
}
