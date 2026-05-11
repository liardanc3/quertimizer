package com.quertimizer.community.application.input;

import lombok.Data;

@Data
public class CommunityCommentInput {

    private final Long parentCommentId;
    private final String content;
}
