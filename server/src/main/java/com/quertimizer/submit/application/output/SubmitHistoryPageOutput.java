package com.quertimizer.submit.application.output;

import java.util.List;

public record SubmitHistoryPageOutput(int currentPage,
                                      int pageSize,
                                      int totalCount,
                                      int totalPages,
                                      List<String> problemIds,
                                      List<SubmitHistoryListItemOutput> histories) {
}
