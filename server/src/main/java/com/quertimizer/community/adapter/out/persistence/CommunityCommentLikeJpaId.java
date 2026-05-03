package com.quertimizer.community.adapter.out.persistence;

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
public class CommunityCommentLikeJpaId implements Serializable {

    @Column(name = "comment_id", nullable = false)
    private Long commentId;

    @Column(name = "handle", nullable = false, length = 50)
    private String handle;

    public CommunityCommentLikeJpaId(Long commentId, String handle) {
        this.commentId = commentId;
        this.handle = handle;
    }
}
