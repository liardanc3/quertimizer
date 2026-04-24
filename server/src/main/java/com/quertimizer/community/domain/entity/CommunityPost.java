package com.quertimizer.community.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "community_post")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPost {

    @Id
    @Column(name = "post_id", nullable = false, length = 50)
    private String postId;

    @Column(name = "handle", nullable = false, length = 50)
    private String handle;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "content_html", nullable = false, columnDefinition = "TEXT")
    private String contentHtml;

    @Column(name = "content_text", nullable = false, columnDefinition = "TEXT")
    private String contentText;

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

    public static CommunityPost create(String handle, String title, String contentHtml, String contentText) {
        // 게시글 생성
        return new CommunityPost(
                "community-" + UUID.randomUUID().toString().replace("-", ""),
                handle,
                title,
                contentHtml,
                contentText,
                0,
                0,
                0,
                LocalDateTime.now(),
                null
        );
    }

    public static CommunityPost create(String postId,
                                       String handle,
                                       String title,
                                       String contentHtml,
                                       String contentText,
                                       LocalDateTime createdAt) {
        return new CommunityPost(
                postId,
                handle,
                title,
                contentHtml,
                contentText,
                0,
                0,
                0,
                createdAt,
                null
        );
    }

    public void changeContent(String title, String contentHtml, String contentText) {
        // 게시글 본문 변경
        this.title = title;
        this.contentHtml = contentHtml;
        this.contentText = contentText;
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

    private CommunityPost(String postId,
                          String handle,
                          String title,
                          String contentHtml,
                          String contentText,
                          int viewCount,
                          int likeCount,
                          int commentCount,
                          LocalDateTime createdAt,
                          LocalDateTime updatedAt) {
        this.postId = postId;
        this.handle = handle;
        this.title = title;
        this.contentHtml = contentHtml;
        this.contentText = contentText;
        this.viewCount = viewCount;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

}
