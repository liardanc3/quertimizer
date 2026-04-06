package com.quertimizer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "post_id", nullable = false, length = 50)
    private String postId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static CommunityComment create(String postId, String userId, Long parentCommentId, String content) {
        return new CommunityComment(
                postId,
                userId,
                parentCommentId,
                content,
                0,
                LocalDateTime.now()
        );
    }

    public void increaseLikeCount() {
        this.likeCount += 1;
    }

    public void decreaseLikeCount() {
        this.likeCount = Math.max(0, this.likeCount - 1);
    }

    private CommunityComment(String postId,
                             String userId,
                             Long parentCommentId,
                             String content,
                             int likeCount,
                             LocalDateTime createdAt) {
        this.postId = postId;
        this.userId = userId;
        this.parentCommentId = parentCommentId;
        this.content = content;
        this.likeCount = likeCount;
        this.createdAt = createdAt;
    }

}
