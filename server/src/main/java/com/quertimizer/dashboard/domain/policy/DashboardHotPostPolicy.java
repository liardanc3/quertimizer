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

    /**
     * 대시보드 인기 게시글 표시 개수를 반환한다.
     *
     * @return 인기 게시글 표시 개수
     */
    public int getDisplayLimit() {
        return DISPLAY_LIMIT;
    }

    /**
     * 인기 게시글 정렬 기준을 생성한다.
     *
     * @return 인기 점수와 생성 시각 기준 비교자
     */
    public Comparator<CommunityPost> createHotPostComparator() {
        return Comparator.comparingDouble(this::calculateHotScore)
                .reversed()
                .thenComparing(CommunityPost::getCreatedAt, Comparator.reverseOrder())
                .thenComparing(CommunityPost::getPostId);
    }

    /**
     * 게시글의 대시보드 인기 점수를 계산한다.
     *
     * @param post 인기 점수를 계산할 게시글
     * @return 좋아요, 댓글, 조회수, 최신성 기반 인기 점수
     */
    public double calculateHotScore(CommunityPost post) {
        long elapsedHours = Math.max(0L, Duration.between(post.getCreatedAt(), LocalDateTime.now()).toHours());
        double recencyBonus = Math.max(0L, RECENCY_BONUS_HOURS - elapsedHours) * RECENCY_WEIGHT;

        return post.getLikeCount() * LIKE_WEIGHT
                + post.getCommentCount() * COMMENT_WEIGHT
                + Math.sqrt(Math.max(0, post.getViewCount())) * VIEW_WEIGHT
                + recencyBonus;
    }

}
