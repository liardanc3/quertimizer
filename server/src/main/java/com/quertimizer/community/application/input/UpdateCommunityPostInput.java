package com.quertimizer.community.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UpdateCommunityPostInput {

    private final Long postId;
    private final String handle;
    private final CommunityPostInput post;
}
