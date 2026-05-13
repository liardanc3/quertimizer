package com.quertimizer.problem.adapter.in.http.response;

import com.quertimizer.problem.application.output.AdminProblemOptionOutput;
import lombok.Data;

@Data
public class AdminProblemOptionRes {

    private final String problemId;

    public static AdminProblemOptionRes from(AdminProblemOptionOutput result) {
        return new AdminProblemOptionRes(result.problemId());
    }
}
