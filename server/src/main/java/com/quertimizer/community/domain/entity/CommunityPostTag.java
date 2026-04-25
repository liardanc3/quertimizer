package com.quertimizer.community.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "community_post_tag")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPostTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(nullable = false, length = 100)
    private String tag;

    @Column(name = "tag_order", nullable = false)
    private int tagOrder;

    public static CommunityPostTag create(Long postId, String tag, int tagOrder) {
        // 게시글 태그 생성
        return new CommunityPostTag(postId, tag, tagOrder);
    }

    private CommunityPostTag(Long postId, String tag, int tagOrder) {
        this.postId = postId;
        this.tag = tag;
        this.tagOrder = tagOrder;
    }

}
