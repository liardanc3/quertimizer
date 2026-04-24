package com.quertimizer.community.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommunityTagSuggestionOutput {

    private final String tag;
    private final long usageCount;
}
