package com.quertimizer.community.domain.entity;

import com.quertimizer.community.domain.entity.ids.CommunityPostLikeId;
import com.quertimizer.user.domain.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private CommunityPost post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "handle", referencedColumnName = "handle", insertable = false, updatable = false)
    private User user;

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
