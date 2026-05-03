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

@Entity
@Table(name = "community_post_tag")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityPostTagJpaEntity {

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

    public static CommunityPostTagJpaEntity create(Long postId, String tag, int tagOrder) {
        // 게시글 태그 JPA 엔티티 생성
        return new CommunityPostTagJpaEntity(null, postId, tag, tagOrder);
    }

    private CommunityPostTagJpaEntity(Long tagId, Long postId, String tag, int tagOrder) {
        this.tagId = tagId;
        this.postId = postId;
        this.tag = tag;
        this.tagOrder = tagOrder;
    }
}
