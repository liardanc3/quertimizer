package com.quertimizer.community.domain.entity;

import com.quertimizer.community.domain.entity.ids.CommunityPostLikeId;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CommunityPostLike {

    private CommunityPostLikeId id;
    private LocalDateTime createdAt;

    public static CommunityPostLike create(Long postId, String handle) {
        // 게시글 좋아요 생성
        return new CommunityPostLike(new CommunityPostLikeId(postId, handle), LocalDateTime.now());
    }

    public static CommunityPostLike restore(Long postId, String handle, LocalDateTime createdAt) {
        // 저장된 게시글 좋아요 상태 복원
        return new CommunityPostLike(new CommunityPostLikeId(postId, handle), createdAt);
    }

    private CommunityPostLike(CommunityPostLikeId id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

}
