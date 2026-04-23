package com.quertimizer.community.domain.entity;

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

    @Column(name = "handle", nullable = false, length = 50)
    private String handle;

    public CommunityPostLikeId(String postId, String handle) {
        this.postId = postId;
        this.handle = handle;
    }

}
