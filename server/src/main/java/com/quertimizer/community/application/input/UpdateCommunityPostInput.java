package com.quertimizer.community.application.input;

import lombok.Data;

@Data
public class UpdateCommunityPostInput {

    private final Long postId;
    private final String handle;
    private final CommunityPostInput post;
}
