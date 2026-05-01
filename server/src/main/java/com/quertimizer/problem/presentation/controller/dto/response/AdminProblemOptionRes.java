package com.quertimizer.problem.presentation.controller.dto.response;

import com.quertimizer.problem.application.output.AdminProblemOptionOutput;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminProblemOptionRes {

    private final String problemId;

    public static AdminProblemOptionRes from(AdminProblemOptionOutput result) {
        return new AdminProblemOptionRes(result.problemId());
    }
}
