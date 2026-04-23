package com.quertimizer.problem.presentation.dto.response;

import com.quertimizer.problem.application.result.ProblemCreateResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProblemCreateRes {

    private final String problemId;

    public static ProblemCreateRes from(ProblemCreateResult result) {
        return new ProblemCreateRes(result.problemId());
    }
}
