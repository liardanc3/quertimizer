package com.quertimizer.community.application.output;

import lombok.Data;

@Data
public class CommunityUserCountsOutput {

    private final long authoredPostCount;
    private final long likedPostCount;
    private final long authoredCommentCount;
}
