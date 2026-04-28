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

    /**
     * 문제 테이블셋 목록을 조회한다.
     *
     * @param authenticatedEmail 조회 권한을 확인할 인증 이메일
     */
    public List<ProblemSetSummaryOutput> execute(String authenticatedEmail) {
        return problemService.getProblemSets(authenticatedEmail);
    }
}
