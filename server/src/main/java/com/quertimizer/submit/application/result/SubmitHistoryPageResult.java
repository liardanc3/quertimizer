package com.quertimizer.submit.application.result;

import java.util.List;

public record SubmitHistoryPageResult(int currentPage,
                                      int pageSize,
                                      int totalCount,
                                      int totalPages,
                                      List<String> problemIds,
                                      List<SubmitHistoryListItemResult> histories) {
}
