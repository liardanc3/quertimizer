package com.quertimizer.problem.application.usecase;

import com.quertimizer.problem.application.input.ProblemSearchInput;
import com.quertimizer.problem.application.output.ProblemPageOutput;
import com.quertimizer.problem.application.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetProblems {

    private final ProblemService problemService;

    /**
     * 문제 목록을 검색 조건에 맞게 조회한다.
     *
     * @param input 문제 목록 검색, 필터, 정렬 입력
     */
    public ProblemPageOutput execute(ProblemSearchInput input) {
        return problemService.getProblems(
                input.getPage(),
                input.getQuery(),
                input.getDbms(),
                input.getSolveState(),
                input.getCurrentHandle(),
                input.getSolvedCountSort(),
                input.getTotalSubmitSort(),
                input.getSuccessSubmitSort(),
                input.getSpreadRateSort(),
                input.getSpreadRateMin(),
                input.getSpreadRateMax()
        );
    }
}
