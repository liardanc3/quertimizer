package com.quertimizer.endpoint.api.dto.response;

import com.quertimizer.entity.Problem;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminProblemOptionRes {

    private final String problemId;

    public static AdminProblemOptionRes from(Problem problem) {
        return new AdminProblemOptionRes(problem.getProblemId());
    }
}
