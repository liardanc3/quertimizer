package com.quertimizer.problem.application.usecase;

import com.quertimizer.problem.application.input.ProblemSetAccessInput;
import com.quertimizer.problem.application.output.ProblemSetDetailOutput;
import com.quertimizer.problem.application.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetProblemSet {

    private final ProblemService problemService;

    /**
     * 문제 테이블셋 상세를 조회한다.
     *
     * @param input 조회할 문제 테이블셋과 인증 이메일 입력
     */
    public Optional<ProblemSetDetailOutput> execute(ProblemSetAccessInput input) {
        return problemService.getProblemSet(input.getProblemSetId(), input.getAuthenticatedEmail());
    }
}
