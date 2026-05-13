package com.quertimizer.community.domain.policy;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class CommunityViewPolicy {

    private static final Duration DUPLICATE_VIEW_WINDOW = Duration.ofMinutes(10);

    private final Map<String, Instant> viewedAtByKey = new ConcurrentHashMap<>();

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
