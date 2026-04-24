package com.quertimizer.problem.application.usecase;

import com.quertimizer.problem.application.input.ProblemCreateInput;
import com.quertimizer.problem.application.output.ProblemCreateOutput;
import com.quertimizer.problem.application.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateProblem {

    private final ProblemService problemService;

    public ProblemCreateOutput execute(ProblemCreateInput input, String authenticatedEmail) {
        // 새 문제를 생성
        return problemService.createProblem(input, authenticatedEmail);
    }
}
