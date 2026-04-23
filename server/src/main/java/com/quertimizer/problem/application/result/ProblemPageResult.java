package com.quertimizer.problem.application.result;

import java.util.List;

public record ProblemPageResult(int currentPage,
                                int pageSize,
                                int totalCount,
                                int totalPages,
                                double spreadRateMin,
                                double spreadRateMax,
                                List<ProblemListItemResult> problems) {
}
