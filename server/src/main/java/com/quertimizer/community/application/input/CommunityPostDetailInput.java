package com.quertimizer.community.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CommunityPostDetailInput {

    private final Long postId;
    private final String currentHandle;
}
