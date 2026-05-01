package com.quertimizer.community.domain.policy;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CommunityViewPolicy {

    private static final Duration DUPLICATE_VIEW_WINDOW = Duration.ofMinutes(10);

    private final Map<String, Instant> viewedAtByKey = new ConcurrentHashMap<>();

    /**
     * 게시글 조회수를 증가시켜야 하는지 판단한다.
     *
     * <ol>
     *   <li>게시글과 조회자 기준 중복 조회 키 생성
     *   <li>기존 조회 시각 확인
     *   <li>중복 조회 창 기준 증가 여부 결정
     * </ol>
     *
     * @param postId 조회한 게시글 번호
     * @param viewerKey 조회자를 구분할 식별자
     * @return 조회수 증가 여부
     */
    public boolean shouldIncreaseViewCount(Long postId, String viewerKey) {
        String key = postId + ":" + normalizeViewerKey(viewerKey);
        Instant now = Instant.now();
        Instant previousViewedAt = viewedAtByKey.put(key, now);
        if (previousViewedAt == null) {
            return true;
        }

        if (previousViewedAt.isBefore(now.minus(DUPLICATE_VIEW_WINDOW))) {
            return true;
        }

        viewedAtByKey.put(key, previousViewedAt);
        return false;
    }

    private String normalizeViewerKey(String viewerKey) {
        return viewerKey == null || viewerKey.isBlank() ? "unknown" : viewerKey.trim();
    }
}
