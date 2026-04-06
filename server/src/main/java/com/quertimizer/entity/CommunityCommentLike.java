package com.quertimizer.entity;

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
public class CommunityCommentLike {

    @EmbeddedId
    private CommunityCommentLikeId id;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static CommunityCommentLike create(Long commentId, String userId) {
        return new CommunityCommentLike(new CommunityCommentLikeId(commentId, userId), LocalDateTime.now());
    }

    private CommunityCommentLike(CommunityCommentLikeId id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

}
