package com.quertimizer.endpoint.api.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class CommunityTagSuggestionRes {

    private final String tag;
    private final long usageCount;

}
