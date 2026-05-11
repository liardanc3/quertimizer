package com.quertimizer.community.application.output;

import lombok.Data;

@Data
public class CommunityReactionOutput {

    private final boolean liked;
    private final int likeCount;
}
