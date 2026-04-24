package com.quertimizer.problem.presentation.dto.response;

import com.quertimizer.problem.application.output.ProblemCreateOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProblemCreateRes {

    private final String problemId;

    public static ProblemCreateRes from(ProblemCreateOutput result) {
        return new ProblemCreateRes(result.problemId());
    }
}
