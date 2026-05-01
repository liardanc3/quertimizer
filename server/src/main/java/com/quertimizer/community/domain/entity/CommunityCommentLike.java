package com.quertimizer.community.domain.entity;

import com.quertimizer.community.domain.entity.ids.CommunityCommentLikeId;
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
@Table(name = "community_comment_like")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityCommentLike {

    @EmbeddedId
    private CommunityCommentLikeId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", insertable = false, updatable = false)
    private CommunityComment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "handle", referencedColumnName = "handle", insertable = false, updatable = false)
    private User user;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static CommunityCommentLike create(Long commentId, String handle) {
        // 댓글 좋아요 생성
        return new CommunityCommentLike(new CommunityCommentLikeId(commentId, handle), LocalDateTime.now());
    }

    private CommunityCommentLike(CommunityCommentLikeId id, LocalDateTime createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

}
