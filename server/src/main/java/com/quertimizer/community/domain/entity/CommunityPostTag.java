package com.quertimizer.community.domain.entity;

import lombok.Getter;

@Getter
public class CommunityPostTag {

    private Long tagId;
    private Long postId;
    private String tag;
    private int tagOrder;

    public static CommunityPostTag create(Long postId, String tag, int tagOrder) {
        // 게시글 태그 생성
        return new CommunityPostTag(postId, tag, tagOrder);
    }

    public static CommunityPostTag restore(Long tagId, Long postId, String tag, int tagOrder) {
        // 저장된 게시글 태그 상태 복원
        CommunityPostTag postTag = new CommunityPostTag(postId, tag, tagOrder);
        postTag.tagId = tagId;
        return postTag;
    }

    private CommunityPostTag(Long postId, String tag, int tagOrder) {
        this.postId = postId;
        this.tag = tag;
        this.tagOrder = tagOrder;
    }

}
