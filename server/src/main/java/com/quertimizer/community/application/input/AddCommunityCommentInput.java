package com.quertimizer.community.application.input;

import lombok.Data;

@Data
public class AddCommunityCommentInput {

    private final Long postId;
    private final String handle;
    private final CommunityCommentInput comment;
}
