package com.quertimizer.problem.adapter.in.http.response;

import com.quertimizer.problem.application.output.ProblemCreateOutput;
import lombok.Data;

@Data
public class ProblemCreateRes {

    private final String problemId;

    public static ProblemCreateRes from(ProblemCreateOutput result) {
        return new ProblemCreateRes(result.problemId());
    }
}
