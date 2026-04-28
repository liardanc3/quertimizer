package com.quertimizer.community.application.input;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class ToggleCommunityCommentLikeInput {

    private final Long commentId;
    private final String handle;
}
