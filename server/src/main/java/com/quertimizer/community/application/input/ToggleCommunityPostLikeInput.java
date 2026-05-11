package com.quertimizer.community.application.input;

import lombok.Data;

@Data
public class ToggleCommunityPostLikeInput {

    private final Long postId;
    private final String handle;
}
