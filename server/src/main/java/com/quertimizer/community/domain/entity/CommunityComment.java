package com.quertimizer.community.domain.entity;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CommunityComment {

    private Long commentId;
    private Long postId;
    private String handle;
    private Long parentCommentId;
    private String content;
    private int likeCount;
    private LocalDateTime createdAt;

    public static CommunityComment create(Long postId, String handle, Long parentCommentId, String content) {
        // 댓글 생성
        return new CommunityComment(
                postId,
                handle,
                parentCommentId,
                content,
                0,
                LocalDateTime.now()
        );
    }

    public static CommunityComment restore(Long commentId, Long postId,
                                           String handle, Long parentCommentId,
                                           String content, int likeCount,
                                           LocalDateTime createdAt) {
        // 저장된 댓글 상태 복원
        CommunityComment comment = new CommunityComment(postId, handle, parentCommentId, content, likeCount, createdAt);
        comment.commentId = commentId;
        return comment;
    }

    public static CommunityComment create(Long postId, String handle,
                                          Long parentCommentId,
                                          String content,
                                          LocalDateTime createdAt) {
        return new CommunityComment(
                postId,
                handle,
                parentCommentId,
                content,
                0,
                createdAt
        );
    }

    public void increaseLikeCount() {
        // 좋아요 수 증가
        this.likeCount += 1;
    }

    public void decreaseLikeCount() {
        // 좋아요 수 감소
        this.likeCount = Math.max(0, this.likeCount - 1);
    }

    private CommunityComment(Long postId, String handle,
                             Long parentCommentId,
                             String content,
                             int likeCount,
                             LocalDateTime createdAt) {
        this.postId = postId;
        this.handle = handle;
        this.parentCommentId = parentCommentId;
        this.content = content;
        this.likeCount = likeCount;
        this.createdAt = createdAt;
    }

}
