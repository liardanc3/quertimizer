package com.quertimizer.community.presentation.controller.dto.response;

import com.quertimizer.community.application.output.CommunityTagSuggestionOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommunityTagSuggestionRes {

    private final String tag;
    private final long usageCount;

    public static CommunityTagSuggestionRes from(CommunityTagSuggestionOutput result) {
        return new CommunityTagSuggestionRes(result.getTag(), result.getUsageCount());
    }
}
