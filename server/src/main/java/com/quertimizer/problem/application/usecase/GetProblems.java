package com.quertimizer.problem.application.usecase;

import com.quertimizer.problem.application.input.ProblemSearchInput;
import com.quertimizer.problem.application.output.ProblemListItemOutput;
import com.quertimizer.problem.application.output.ProblemPageOutput;
import com.quertimizer.problem.application.service.ProblemService;
import com.quertimizer.problem.application.store.ProblemStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetProblems {

    private final ProblemStore problemStore;
    private final ProblemService problemService;

    /**
     * 문제 목록을 검색 조건에 맞게 조회한다.
     *
     * <ol>
     *   <li>검색 조건 기준 문제 페이지 조회
     *   <li>목록 항목 응답 변환
     *   <li>페이지 응답 생성
     * </ol>
     *
     * @param input 문제 목록 검색, 필터, 정렬 입력
     */
    public ProblemPageOutput execute(ProblemSearchInput input) {
        ProblemStore.ProblemPage problemPage = problemStore.findProblemPage(
                input.getPage(), input.getQuery(), problemService.resolveDbmsType(input.getDbms()),
                input.getSolveState(), input.getCurrentHandle(),
                input.getSolvedCountSort(), input.getTotalSubmitSort(), input.getSuccessSubmitSort(),
                input.getSpreadRateSort(), input.getSpreadRateMin(), input.getSpreadRateMax()
        );

        List<ProblemListItemOutput> problems = problemPage.problems().stream()
                .map(problemService::toProblemListItemOutput)
                .toList();

        return new ProblemPageOutput(
                problemPage.currentPage(), problemPage.pageSize(),
                problemPage.totalCount(), problemPage.totalPages(),
                problemPage.spreadRateMin(), problemPage.spreadRateMax(), problems
        );
    }
}
