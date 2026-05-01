package com.quertimizer.community.domain.entity.ids;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CommunityCommentLikeId implements Serializable {

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "handle", nullable = false, length = 50)
    private String handle;

    public CommunityCommentLikeId(Long commentId, String handle) {
        this.commentId = commentId;
        this.handle = handle;
    }

}
