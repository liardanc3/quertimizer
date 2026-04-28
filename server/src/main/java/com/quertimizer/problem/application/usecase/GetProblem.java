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

    /**
     * 문제 상세를 조회한다.
     *
     * @param problemId 조회할 문제 번호
     */
    public Optional<ProblemDetailOutput> execute(String problemId) {
        return problemService.getProblem(problemId);
    }
}
