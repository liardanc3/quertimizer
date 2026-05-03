package com.quertimizer.community.domain.policy;

import java.util.Optional;

import static com.quertimizer.community.domain.model.CommunityPostIdConstant.FIRST_SEED_POST_ID;
import static com.quertimizer.community.domain.model.CommunityPostIdConstant.LAST_SEED_POST_ID;

public final class CommunityPostIdPolicy {
    private CommunityPostIdPolicy() {
    }

    public static String format(Long postId) {
        return postId == null ? "" : "%09d".formatted(postId);
    }

    public static boolean isSeedPostId(Long postId) {
        return postId != null && postId >= FIRST_SEED_POST_ID && postId <= LAST_SEED_POST_ID;
    }

    public static Optional<Integer> resolveSeedPostNumber(Long postId) {
        return isSeedPostId(postId) ? Optional.of(postId.intValue()) : Optional.empty();
    }
}
