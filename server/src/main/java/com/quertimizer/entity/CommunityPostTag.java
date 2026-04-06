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

@Entity
@Table(name = "community_post_tag")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPostTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id", nullable = false)
    private Long tagId;

    @Column(name = "post_id", nullable = false, length = 50)
    private String postId;

    @Column(nullable = false, length = 100)
    private String tag;

    @Column(name = "tag_order", nullable = false)
    private int tagOrder;

    public static CommunityPostTag create(String postId, String tag, int tagOrder) {
        return new CommunityPostTag(postId, tag, tagOrder);
    }

    private CommunityPostTag(String postId, String tag, int tagOrder) {
        this.postId = postId;
        this.tag = tag;
        this.tagOrder = tagOrder;
    }

}
