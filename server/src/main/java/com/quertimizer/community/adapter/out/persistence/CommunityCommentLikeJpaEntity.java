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
@Table(name = "community_comment_like")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityCommentLikeJpaEntity {

    @EmbeddedId
    private CommunityCommentLikeJpaId id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static CommunityCommentLikeJpaEntity create(Long commentId, String handle, LocalDateTime createdAt) {
        // 댓글 좋아요 JPA 엔티티 생성
        return new CommunityCommentLikeJpaEntity(new CommunityCommentLikeJpaId(commentId, handle), createdAt);
    }

    private CommunityCommentLikeJpaEntity(CommunityCommentLikeJpaId id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }
}
