package com.quertimizer.problem.application.output;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.List;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor
public class ProblemListItemOutput {

    private final String problemId;
    private final String title;
    private final String description;
    private final int totalSubmitCount;
    private final int successSubmitCount;
    private final double spreadRate;
    private final List<ProblemSubmittedHistoryOutput> submittedHistories;
}
