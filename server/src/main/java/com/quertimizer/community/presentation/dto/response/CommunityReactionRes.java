package com.quertimizer.community.presentation.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommunityReactionRes {

    private final boolean liked;
    private final int likeCount;

}
