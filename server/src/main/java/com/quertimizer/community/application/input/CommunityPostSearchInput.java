package com.quertimizer.community.application.input;

import lombok.Data;

@Data
public class CommunityPostSearchInput {

    private final int page;
    private final String search;
    private final String tag;
    private final String category;
    private final String sortKey;
}
