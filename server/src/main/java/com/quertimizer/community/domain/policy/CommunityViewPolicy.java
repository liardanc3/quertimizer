package com.quertimizer.community.domain.policy;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.quertimizer.community.domain.model.CommunityViewConstant.DUPLICATE_VIEW_WINDOW;

public class CommunityViewPolicy {
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
