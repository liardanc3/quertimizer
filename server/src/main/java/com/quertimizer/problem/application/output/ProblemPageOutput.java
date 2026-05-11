package com.quertimizer.problem.application.output;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(fluent = true)
public class ProblemPageOutput {

    private final int currentPage;
    private final int pageSize;
    private final int totalCount;
    private final int totalPages;
    private final double spreadRateMin;
    private final double spreadRateMax;
    private final List<ProblemListItemOutput> problems;
}
