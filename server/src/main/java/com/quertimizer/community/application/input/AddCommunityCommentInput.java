package com.quertimizer.community.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class AddCommunityCommentInput {

    private final Long postId;
    private final String handle;
    private final CommunityCommentInput comment;
}
