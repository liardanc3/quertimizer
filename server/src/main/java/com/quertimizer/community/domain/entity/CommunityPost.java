package com.quertimizer.community.domain.entity;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class CommunityPost {

    private Long postId;
    private String handle;
    private String title;
    private String contentJson;
    private String plainTextSummary;
    private String imageIds;
    private String category;
    private int viewCount;
    private int likeCount;
    private int commentCount;
    private LocalDateTime createdAt;
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

    public static CommunityPost restore(Long postId, String handle,
                                        String title, String contentJson,
                                        String plainTextSummary, String imageIds,
                                        String category, int viewCount,
                                        int likeCount, int commentCount,
                                        LocalDateTime createdAt,
                                        LocalDateTime updatedAt) {
        // 저장된 게시글 상태 복원
        return new CommunityPost(
                postId, handle, title, contentJson, plainTextSummary,
                imageIds, category, viewCount, likeCount,
                commentCount, createdAt, updatedAt
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
