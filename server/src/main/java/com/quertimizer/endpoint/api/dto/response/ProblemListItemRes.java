package com.quertimizer.endpoint.api.dto.response;

import com.quertimizer.entity.Problem;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ProblemListItemRes {

    private final String problemId;
    private final String title;
    private final String description;
    private final List<ProblemSubmittedHistoryRes> submittedHistories;

    public static ProblemListItemRes of(Problem problem, List<ProblemSubmittedHistoryRes> submittedHistories) {
        return new ProblemListItemRes(
                problem.getProblemId(),
                problem.getTitle(),
                problem.getDescription(),
                submittedHistories
        );
    }

}
