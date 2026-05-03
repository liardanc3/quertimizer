package com.quertimizer.community.adapter.out.persistence;

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
public class CommunityCommentJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "handle", nullable = false, length = 50)
    private String handle;

    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static CommunityCommentJpaEntity create(Long postId, String handle,
                                                   Long parentCommentId, String content,
                                                   int likeCount, LocalDateTime createdAt) {
        // 댓글 JPA 엔티티 생성
        return new CommunityCommentJpaEntity(null, postId, handle, parentCommentId, content, likeCount, createdAt);
    }

    public void update(String content, int likeCount) {
        // 댓글 JPA 엔티티 내용 변경
        this.content = content;
        this.likeCount = likeCount;
    }

    private CommunityCommentJpaEntity(Long commentId, Long postId, String handle,
                                      Long parentCommentId, String content,
                                      int likeCount, LocalDateTime createdAt) {
        this.commentId = commentId;
        this.postId = postId;
        this.handle = handle;
        this.parentCommentId = parentCommentId;
        this.content = content;
        this.likeCount = likeCount;
        this.createdAt = createdAt;
    }
}
