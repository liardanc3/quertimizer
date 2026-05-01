package com.quertimizer.community.domain.entity;

import com.quertimizer.user.domain.entity.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
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
@Table(name = "community_post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPost {

    @Id
    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "handle", nullable = false, length = 50)
    private String handle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "handle", referencedColumnName = "handle", insertable = false, updatable = false)
    private User author;

    @BatchSize(size = 50)
    @OrderBy("createdAt ASC")
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommunityComment> comments = new ArrayList<>();

    @BatchSize(size = 50)
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommunityPostLike> likes = new ArrayList<>();

    @BatchSize(size = 50)
    @OrderBy("tagOrder ASC")
    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CommunityPostTag> tags = new ArrayList<>();

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "content_json", nullable = false, columnDefinition = "TEXT")
    private String contentJson;

    @Column(name = "plain_text_summary", nullable = false, columnDefinition = "TEXT")
    private String plainTextSummary;

    @Column(name = "image_ids", nullable = false, columnDefinition = "TEXT")
    private String imageIds;

    @Column(length = 20)
    private String category;

    @Column(name = "view_count", nullable = false)
    private int viewCount;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "comment_count", nullable = false)
    private int commentCount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public static CommunityPost create(Long postId,
                                       String handle,
                                       String title,
                                       String contentJson,
                                       String plainTextSummary,
                                       String imageIds,
                                       String category) {
        // 게시글 생성
        return new CommunityPost(
                postId,
                handle,
                title,
                contentJson,
                plainTextSummary,
                imageIds,
                category,
                0,
                0,
                0,
                LocalDateTime.now(),
                null
        );
    }

    public static CommunityPost create(Long postId, String handle,
                                       String title,
                                       String contentJson,
                                       String plainTextSummary,
                                       String imageIds,
                                       String category,
                                       LocalDateTime createdAt) {
        return new CommunityPost(
                postId,
                handle,
                title,
                contentJson,
                plainTextSummary,
                imageIds,
                category,
                0,
                0,
                0,
                createdAt,
                null
        );
    }

    public void changeContent(String title, String contentJson, String plainTextSummary, String imageIds, String category) {
        // 게시글 본문 변경
        this.title = title;
        this.contentJson = contentJson;
        this.plainTextSummary = plainTextSummary;
        this.imageIds = imageIds;
        this.category = category;
        this.updatedAt = LocalDateTime.now();
    }

    public void increaseViewCount() {
        // 조회수 증가
        this.viewCount += 1;
    }

    public void increaseLikeCount() {
        // 좋아요 수 증가
        this.likeCount += 1;
    }

    public void decreaseLikeCount() {
        // 좋아요 수 감소
        this.likeCount = Math.max(0, this.likeCount - 1);
    }

    public void increaseCommentCount() {
        // 댓글 수 증가
        this.commentCount += 1;
    }

    public void decreaseCommentCount(int amount) {
        // 댓글 수 감소
        this.commentCount = Math.max(0, this.commentCount - Math.max(amount, 0));
    }

    private CommunityPost(Long postId, String handle,
                          String title,
                          String contentJson,
                          String plainTextSummary,
                          String imageIds,
                          String category,
                          int viewCount,
                          int likeCount,
                          int commentCount,
                          LocalDateTime createdAt,
                          LocalDateTime updatedAt) {
        this.postId = postId;
        this.handle = handle;
        this.title = title;
        this.contentJson = contentJson;
        this.plainTextSummary = plainTextSummary;
        this.imageIds = imageIds;
        this.category = category;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

}
