package com.quertimizer.dashboard.domain.policy;

import com.quertimizer.community.domain.entity.CommunityPost;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;

@Component
public class DashboardHotPostPolicy {

    private static final int DISPLAY_LIMIT = 11;
    private static final double LIKE_WEIGHT = 5d;
    private static final double COMMENT_WEIGHT = 4d;
    private static final double VIEW_WEIGHT = 0.35d;
    private static final double RECENCY_WEIGHT = 0.8d;
    private static final long RECENCY_BONUS_HOURS = 72L;

    public int getDisplayLimit() {
        // 표시 개수 조회
        return DISPLAY_LIMIT;
    }

    public Comparator<CommunityPost> createHotPostComparator() {
        // 인기 게시글 비교 기준 생성
        return Comparator.comparingDouble(this::calculateHotScore)
                .reversed()
                .thenComparing(CommunityPost::getCreatedAt, Comparator.reverseOrder())
                .thenComparing(CommunityPost::getPostId);
    }

    public double calculateHotScore(CommunityPost post) {
        // 인기 점수 계산
        long elapsedHours = Math.max(0L, Duration.between(post.getCreatedAt(), LocalDateTime.now()).toHours());
        double recencyBonus = Math.max(0L, RECENCY_BONUS_HOURS - elapsedHours) * RECENCY_WEIGHT;

        return post.getLikeCount() * LIKE_WEIGHT
                + post.getCommentCount() * COMMENT_WEIGHT
                + Math.sqrt(Math.max(0, post.getViewCount())) * VIEW_WEIGHT
                + recencyBonus;
    }

}
