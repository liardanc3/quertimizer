package com.quertimizer.ranking.application.output;

import java.util.List;

public record RankPageOutput(int currentPage,
                             int pageSize,
                             int totalCount,
                             int totalPages,
                             List<RankListItemOutput> ranks) {
}
