package com.quertimizer.problem.application.output;

import java.util.List;

public record ProblemListItemOutput(String problemId,
                                    String title,
                                    String description,
                                    int totalSubmitCount,
                                    int successSubmitCount,
                                    double spreadRate,
                                    List<ProblemSubmittedHistoryOutput> submittedHistories) {
}
