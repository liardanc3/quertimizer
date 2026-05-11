package com.quertimizer.community.adapter.in.web.response;

import com.quertimizer.community.application.output.CommunityReactionOutput;
import lombok.Data;

@Data
public class CommunityReactionRes {

    private final boolean liked;
    private final int likeCount;

    public static CommunityReactionRes from(CommunityReactionOutput result) {
        return new CommunityReactionRes(result.isLiked(), result.getLikeCount());
    }
}
