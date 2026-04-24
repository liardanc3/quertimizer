package com.quertimizer.problem.application.output;

import java.util.List;

public record ProblemPageOutput(int currentPage,
                                int pageSize,
                                int totalCount,
                                int totalPages,
                                double spreadRateMin,
                                double spreadRateMax,
                                List<ProblemListItemOutput> problems) {
}
