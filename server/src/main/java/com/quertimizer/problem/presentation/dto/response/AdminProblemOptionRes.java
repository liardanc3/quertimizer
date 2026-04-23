package com.quertimizer.problem.presentation.dto.response;

import com.quertimizer.problem.application.result.AdminProblemOptionResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AdminProblemOptionRes {

    private final String problemId;

    public static AdminProblemOptionRes from(AdminProblemOptionResult result) {
        return new AdminProblemOptionRes(result.problemId());
    }
}
