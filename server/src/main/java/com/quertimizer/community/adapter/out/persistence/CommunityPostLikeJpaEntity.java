package com.quertimizer.community.adapter.out.persistence;

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
public class CommunityPostLikeJpaEntity {

    @EmbeddedId
    private CommunityPostLikeJpaId id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static CommunityPostLikeJpaEntity create(Long postId, String handle, LocalDateTime createdAt) {
        // 게시글 좋아요 JPA 엔티티 생성
        return new CommunityPostLikeJpaEntity(new CommunityPostLikeJpaId(postId, handle), createdAt);
    }

    private CommunityPostLikeJpaEntity(CommunityPostLikeJpaId id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }
}
