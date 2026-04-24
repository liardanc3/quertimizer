package com.quertimizer.problem.application.usecase;

import com.quertimizer.problem.application.output.ProblemSetSummaryOutput;
import com.quertimizer.problem.application.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetProblemSets {

    private final ProblemService problemService;

    public List<ProblemSetSummaryOutput> execute(String authenticatedEmail) {
        // 문제 테이블셋 목록을 조회
        return problemService.getProblemSets(authenticatedEmail);
    }
}
