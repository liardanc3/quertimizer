package com.quertimizer.ranking.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class RankPageOutput {

    private final int currentPage;
    private final int pageSize;
    private final int totalCount;
    private final int totalPages;
    private final List<RankListItemOutput> ranks;
}
