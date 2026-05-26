package com.quertimizer.community.application.output;

import lombok.Data;

import java.util.List;

@Data
public class CommunityUserActivitiesOutput {

    private final int currentPage;
    private final int pageSize;
    private final int totalCount;
    private final int totalPages;
    private final List<CommunityUserActivityOutput> activities;
}
