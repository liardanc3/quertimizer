package com.quertimizer.community.adapter.out.elasticsearch;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;

import java.time.LocalDateTime;
import java.util.List;

@Document(indexName = "community-post-v2", createIndex = false)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPostDocument {

    @Id
    private String postId;
    private String title;
    private String authorId;
    private String contentText;
    private String commentText;
    private List<String> tags;
    private String category;
    private int likeCount;
    private int commentCount;
    private int viewCount;
    private LocalDateTime createdAt;

    public static CommunityPostDocument create(String postId,
                                               String title,
                                               String authorId,
                                               String contentText,
                                               String commentText,
                                               List<String> tags,
                                               String category,
                                               int likeCount,
                                               int commentCount,
                                               int viewCount,
                                               LocalDateTime createdAt) {
        return new CommunityPostDocument(
                postId,
                title,
                authorId,
                contentText,
                commentText,
                tags,
                category,
                likeCount,
                commentCount,
                viewCount,
                createdAt
        );
    }

    private CommunityPostDocument(String postId,
                                  String title,
                                  String authorId,
                                  String contentText,
                                  String commentText,
                                  List<String> tags,
                                  String category,
                                  int likeCount,
                                  int commentCount,
                                  int viewCount,
                                  LocalDateTime createdAt) {
        this.postId = postId;
        this.title = title;
        this.authorId = authorId;
        this.contentText = contentText;
        this.commentText = commentText;
        this.tags = tags;
        this.category = category;
        this.likeCount = likeCount;
        this.commentCount = commentCount;
        this.viewCount = viewCount;
        this.createdAt = createdAt;
    }

}
