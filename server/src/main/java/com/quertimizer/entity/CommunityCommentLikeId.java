package com.quertimizer.entity;

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

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    public CommunityCommentLikeId(Long commentId, String userId) {
        this.commentId = commentId;
        this.userId = userId;
    }

}
