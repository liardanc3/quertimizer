package com.quertimizer.community.domain.entity.ids;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
public class CommunityPostLikeId implements Serializable {

    private Long postId;
    private String handle;

    public CommunityPostLikeId(Long postId, String handle) {
        this.postId = postId;
        this.handle = handle;
    }

}
