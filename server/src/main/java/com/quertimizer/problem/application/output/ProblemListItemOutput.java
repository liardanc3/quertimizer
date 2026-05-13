package com.quertimizer.problem.application.output;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

@Data
@Accessors(fluent = true)
public class ProblemListItemOutput {

    private final String problemId;
    private final String title;
    private final String description;
    private final int totalSubmitCount;
    private final int successSubmitCount;
    private final List<ProblemSubmittedHistoryOutput> submittedHistories;
}
