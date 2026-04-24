package com.quertimizer.problem.application.usecase;

import com.quertimizer.problem.application.output.AdminProblemOptionOutput;
import com.quertimizer.problem.application.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetProblemOptions {

    private final ProblemService problemService;

    public List<AdminProblemOptionOutput> execute(String problemSetId, String authenticatedEmail) {
        // 문제 관리용 문제 옵션 목록을 조회
        return problemService.getProblemOptions(problemSetId, authenticatedEmail);
    }
}
