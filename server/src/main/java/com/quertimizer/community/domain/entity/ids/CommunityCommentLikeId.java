package com.quertimizer.community.domain.entity.ids;

import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;

@Getter
@EqualsAndHashCode
public class CommunityCommentLikeId implements Serializable {

    private Long commentId;
    private String handle;

    public CommunityCommentLikeId(Long commentId, String handle) {
        this.commentId = commentId;
        this.handle = handle;
    }

}
