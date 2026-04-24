package com.quertimizer.problem.application.usecase;

import com.quertimizer.problem.application.output.ProblemPageOutput;
import com.quertimizer.problem.application.service.ProblemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GetProblems {

    private final ProblemService problemService;

    public ProblemPageOutput execute(int page,
                                     String query,
                                     String dbms,
                                     String solveState,
                                     String currentHandle,
                                     String solvedCountSort,
                                     String totalSubmitSort,
                                     String successSubmitSort,
                                     String spreadRateSort,
                                     Double spreadRateMin,
                                     Double spreadRateMax) {
        // 문제 목록을 조회
        return problemService.getProblems(
                page,
                query,
                dbms,
                solveState,
                currentHandle,
                solvedCountSort,
                totalSubmitSort,
                successSubmitSort,
                spreadRateSort,
                spreadRateMin,
                spreadRateMax
        );
    }
}
