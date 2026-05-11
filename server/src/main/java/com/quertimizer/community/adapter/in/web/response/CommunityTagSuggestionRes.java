package com.quertimizer.community.adapter.in.web.response;

import com.quertimizer.community.application.output.CommunityTagSuggestionOutput;
import lombok.Data;

@Data
public class CommunityTagSuggestionRes {

    private final String tag;
    private final long usageCount;

    public static CommunityTagSuggestionRes from(CommunityTagSuggestionOutput result) {
        return new CommunityTagSuggestionRes(result.getTag(), result.getUsageCount());
    }
}
