package com.quertimizer.community.domain.entity;

import com.quertimizer.user.domain.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "community_comment")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", insertable = false, updatable = false)
    private CommunityPost post;

    @Column(name = "handle", nullable = false, length = 50)
    private String handle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "handle", referencedColumnName = "handle", insertable = false, updatable = false)
    private User author;

    @Column(name = "parent_comment_id")
    private Long parentCommentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id", insertable = false, updatable = false)
    private CommunityComment parentComment;

    @BatchSize(size = 50)
    @OrderBy("createdAt ASC")
    @OneToMany(mappedBy = "parentComment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommunityComment> replies = new ArrayList<>();

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "comment", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommunityCommentLike> likes = new ArrayList<>();

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static CommunityComment create(Long postId, String handle, Long parentCommentId, String content) {
        // 댓글 생성
        return new CommunityComment(
                postId,
                handle,
                parentCommentId,
                content,
                0,
                LocalDateTime.now()
        );
    }

    public static CommunityComment create(Long postId, String handle,
                                          Long parentCommentId,
                                          String content,
                                          LocalDateTime createdAt) {
        return new CommunityComment(
                postId,
                handle,
                parentCommentId,
                content,
                0,
                createdAt
        );
    }

    public void increaseLikeCount() {
        // 좋아요 수 증가
        this.likeCount += 1;
    }

    public void decreaseLikeCount() {
        // 좋아요 수 감소
        this.likeCount = Math.max(0, this.likeCount - 1);
    }

    private CommunityComment(Long postId, String handle,
                             Long parentCommentId,
                             String content,
                             int likeCount,
                             LocalDateTime createdAt) {
        this.postId = postId;
        this.handle = handle;
        this.parentCommentId = parentCommentId;
        this.content = content;
        this.likeCount = likeCount;
        this.createdAt = createdAt;
    }

}
