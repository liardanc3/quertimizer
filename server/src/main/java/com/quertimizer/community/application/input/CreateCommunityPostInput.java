package com.quertimizer.community.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CreateCommunityPostInput {

    private final String handle;
    private final CommunityPostInput post;
}
