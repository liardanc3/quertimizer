package com.quertimizer.problem.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class ProblemPageOutput {

    private final int currentPage;
    private final int pageSize;
    private final int totalCount;
    private final int totalPages;
    private final double spreadRateMin;
    private final double spreadRateMax;
    private final List<ProblemListItemOutput> problems;
}
