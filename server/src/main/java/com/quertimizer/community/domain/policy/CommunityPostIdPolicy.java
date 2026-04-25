package com.quertimizer.community.domain.policy;

import java.util.Optional;

public final class CommunityPostIdPolicy {

    private static final long FIRST_SEED_POST_ID = 1L;
    private static final long LAST_SEED_POST_ID = 34L;

    private CommunityPostIdPolicy() {
    }

    public static String format(Long postId) {
        // 게시글 번호 표시 형식 변환
        return postId == null ? "" : "%09d".formatted(postId);
    }

    public static boolean isSeedPostId(Long postId) {
        // Seed 게시글 여부 확인
        return postId != null && postId >= FIRST_SEED_POST_ID && postId <= LAST_SEED_POST_ID;
    }

    public static Optional<Integer> resolveSeedPostNumber(Long postId) {
        // Seed 게시글 번호 결정
        return isSeedPostId(postId) ? Optional.of(postId.intValue()) : Optional.empty();
    }

}
