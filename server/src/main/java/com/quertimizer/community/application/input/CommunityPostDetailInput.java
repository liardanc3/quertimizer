package com.quertimizer.community.application.input;

import lombok.Data;

@Data
public class CommunityPostDetailInput {

    private final Long postId;
    private final String currentHandle;
    private final String viewerKey;
}
