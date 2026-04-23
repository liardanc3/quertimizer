package com.quertimizer.problem.application.usecase;

import com.quertimizer.problem.application.result.ProblemDetailResult;
import com.quertimizer.problem.application.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetProblem {

    private final ProblemService problemService;

    public Optional<ProblemDetailResult> execute(String problemId) {
        return problemService.getProblem(problemId);
    }

}
