package com.quertimizer.community.application.input;

import lombok.Data;

@Data
public class CreateCommunityPostInput {

    private final String handle;
    private final CommunityPostInput post;
}
