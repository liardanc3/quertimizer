package com.quertimizer.problem.application.usecase;

import com.quertimizer.problem.application.output.ProblemDetailOutput;
import com.quertimizer.problem.application.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetProblem {

    private final ProblemService problemService;

    public Optional<ProblemDetailOutput> execute(String problemId) {
        // 문제 상세를 조회
        return problemService.getProblem(problemId);
    }
}
