package com.quertimizer.community.domain.policy;

import java.util.Optional;

public final class CommunityPostIdPolicy {

    private static final long FIRST_SEED_POST_ID = 1L;
    private static final long LAST_SEED_POST_ID = 5L;

    private CommunityPostIdPolicy() {
    }

    /**
     * 게시글 번호를 화면 표시 형식으로 변환한다.
     *
     * @param postId 표시할 게시글 번호
     * @return 9자리 게시글 번호 문자열
     */
    public static String format(Long postId) {
        return postId == null ? "" : "%09d".formatted(postId);
    }

    /**
     * seed 게시글 번호인지 확인한다.
     *
     * @param postId 확인할 게시글 번호
     * @return seed 게시글 번호 여부
     */
    public static boolean isSeedPostId(Long postId) {
        return postId != null && postId >= FIRST_SEED_POST_ID && postId <= LAST_SEED_POST_ID;
    }

    /**
     * seed 게시글 번호를 기본 카테고리 계산용 번호로 변환한다.
     *
     * @param postId 확인할 게시글 번호
     * @return seed 게시글이면 숫자 값, 아니면 빈 값
     */
    public static Optional<Integer> resolveSeedPostNumber(Long postId) {
        return isSeedPostId(postId) ? Optional.of(postId.intValue()) : Optional.empty();
    }

}
