package com.quertimizer.community.application.output;

import lombok.Data;

@Data
public class CommunityTagSuggestionOutput {

    private final String tag;
    private final long usageCount;
}
