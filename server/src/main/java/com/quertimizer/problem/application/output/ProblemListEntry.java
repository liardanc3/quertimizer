package com.quertimizer.problem.application.output;

import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class ProblemListEntry {

    private final Problem problem;
    private final List<ProblemSolveHistory> submittedHistories;
    private final int solvedUserCount;
    private final boolean solvedByCurrentUser;
    private final int totalSubmitCount;
    private final int successSubmitCount;
    private final double spreadRate;
}
