package com.quertimizer.community.presentation.dto.response;

import com.quertimizer.community.application.output.CommunityReactionOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommunityReactionRes {

    private final boolean liked;
    private final int likeCount;

    public static CommunityReactionRes from(CommunityReactionOutput result) {
        return new CommunityReactionRes(result.isLiked(), result.getLikeCount());
    }
}
