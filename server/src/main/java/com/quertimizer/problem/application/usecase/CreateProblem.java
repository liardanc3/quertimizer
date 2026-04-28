package com.quertimizer.problem.application.usecase;

import com.quertimizer.problem.application.input.CreateProblemInput;
import com.quertimizer.problem.application.output.ProblemCreateOutput;
import com.quertimizer.problem.application.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CreateProblem {

    private final ProblemService problemService;

    /**
     * 관리자 문제를 생성한다.
     *
     * @param input 문제 생성 요청과 인증 이메일 입력
     */
    public ProblemCreateOutput execute(CreateProblemInput input) {
        return problemService.createProblem(input.getProblem(), input.getAuthenticatedEmail());
    }
}
