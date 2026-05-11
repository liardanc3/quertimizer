package com.quertimizer.community.application.input;

import lombok.Data;

@Data
public class ToggleCommunityCommentLikeInput {

    private final Long commentId;
    private final String handle;
}
