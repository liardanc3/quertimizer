package com.quertimizer.community.application.input;

import lombok.Data;

@Data
public class DeleteCommunityPostInput {

    private final Long postId;
    private final String handle;
}
