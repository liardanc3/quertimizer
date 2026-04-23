package com.quertimizer.ranking.application.result;

import java.util.List;

public record RankPageResult(int currentPage,
                             int pageSize,
                             int totalCount,
                             int totalPages,
                             List<RankListItemResult> ranks) {
}
