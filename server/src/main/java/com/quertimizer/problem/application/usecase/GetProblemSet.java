package com.quertimizer.problem.application.usecase;

import com.quertimizer.problem.application.output.ProblemSetDetailOutput;
import com.quertimizer.problem.application.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetProblemSet {

    private final ProblemService problemService;

    public Optional<ProblemSetDetailOutput> execute(String problemSetId, String authenticatedEmail) {
        // 문제 테이블셋 상세를 조회
        return problemService.getProblemSet(problemSetId, authenticatedEmail);
    }
}
