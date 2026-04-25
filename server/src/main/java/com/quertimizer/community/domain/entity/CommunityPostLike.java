package com.quertimizer.community.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_post_like")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPostLike {

    @EmbeddedId
    private CommunityPostLikeId id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static CommunityPostLike create(Long postId, String handle) {
        // 게시글 좋아요 생성
        return new CommunityPostLike(new CommunityPostLikeId(postId, handle), LocalDateTime.now());
    }

    private CommunityPostLike(CommunityPostLikeId id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

}
