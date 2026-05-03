package com.quertimizer.community.domain.entity;

import com.quertimizer.community.domain.entity.ids.CommunityCommentLikeId;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CommunityCommentLike {

    private CommunityCommentLikeId id;
    private LocalDateTime createdAt;

    public static CommunityCommentLike create(Long commentId, String handle) {
        // 댓글 좋아요 생성
        return new CommunityCommentLike(new CommunityCommentLikeId(commentId, handle), LocalDateTime.now());
    }

    public static CommunityCommentLike restore(Long commentId, String handle, LocalDateTime createdAt) {
        // 저장된 댓글 좋아요 상태 복원
        return new CommunityCommentLike(new CommunityCommentLikeId(commentId, handle), createdAt);
    }

    private CommunityCommentLike(CommunityCommentLikeId id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

}
