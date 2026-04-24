package com.quertimizer.community.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommunityReactionOutput {

    private final boolean liked;
    private final int likeCount;
}
