package com.quertimizer.problem.application.result;

import java.util.List;

public record ProblemListItemResult(String problemId,
                                    String title,
                                    String description,
                                    int totalSubmitCount,
                                    int successSubmitCount,
                                    double spreadRate,
                                    List<ProblemSubmittedHistoryResult> submittedHistories) {
}
