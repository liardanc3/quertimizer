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
public class CommunityPostLikeId implements Serializable {

    @Column(name = "post_id", nullable = false, length = 50)
    private String postId;

    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    public CommunityPostLikeId(String postId, String userId) {
        this.postId = postId;
        this.userId = userId;
    }

}
