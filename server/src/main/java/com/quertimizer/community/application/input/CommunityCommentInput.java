package com.quertimizer.community.application.input;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommunityCommentInput {

    private final Long parentCommentId;
    private final String content;
}
