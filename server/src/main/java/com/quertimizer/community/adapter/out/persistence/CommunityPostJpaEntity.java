package com.quertimizer.community.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "community_post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPostJpaEntity {

    @Id
    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "handle", nullable = false, length = 50)
    private String handle;

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

    public static CommunityPostJpaEntity create(Long postId, String handle,
                                                String title, String contentJson,
                                                String plainTextSummary, String imageIds,
                                                String category, int viewCount,
                                                int likeCount, int commentCount,
                                                LocalDateTime createdAt,
                                                LocalDateTime updatedAt) {
        // 게시글 JPA 엔티티 생성
        return new CommunityPostJpaEntity(
                postId, handle, title, contentJson, plainTextSummary,
                imageIds, category, viewCount, likeCount,
                commentCount, createdAt, updatedAt
        );
    }

    public void update(String title, String contentJson,
                       String plainTextSummary, String imageIds,
                       String category, int viewCount,
                       int likeCount, int commentCount,
                       LocalDateTime updatedAt) {
        // 게시글 JPA 엔티티 내용 변경
        this.title = title;
        this.contentJson = contentJson;
        this.plainTextSummary = plainTextSummary;
        this.imageIds = imageIds;
        this.category = category;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.updatedAt = updatedAt;
    }

    private CommunityPostJpaEntity(Long postId, String handle,
                                   String title, String contentJson,
                                   String plainTextSummary, String imageIds,
                                   String category, int viewCount,
                                   int likeCount, int commentCount,
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
