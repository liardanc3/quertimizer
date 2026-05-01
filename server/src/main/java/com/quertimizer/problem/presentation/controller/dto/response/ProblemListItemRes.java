package com.quertimizer.problem.presentation.controller.dto.response;

import com.quertimizer.problem.application.output.ProblemListItemOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ProblemListItemRes {

    private final String problemId;
    private final String title;
    private final String description;
    private final int totalSubmitCount;
    private final int successSubmitCount;
    private final double spreadRate;
    private final List<ProblemSubmittedHistoryRes> submittedHistories;

    public static ProblemListItemRes from(ProblemListItemOutput result) {
        return new ProblemListItemRes(
                result.problemId(), result.title(), result.description(),
                result.totalSubmitCount(), result.successSubmitCount(), result.spreadRate(),
                result.submittedHistories().stream()
                        .map(ProblemSubmittedHistoryRes::from)
                        .toList()
        );
    }

}
