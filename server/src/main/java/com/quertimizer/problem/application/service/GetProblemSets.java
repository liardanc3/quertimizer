package com.quertimizer.problem.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.problem.application.port.in.GetProblemSetsUseCase;
import com.quertimizer.problem.application.output.ProblemSetSummaryOutput;
import com.quertimizer.problem.domain.entity.ProblemSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetProblemSets implements GetProblemSetsUseCase {

    private final ProblemSearchService problemSearchService;

    /**
     * 문제 테이블셋 목록을 조회한다.
     *
     * <ol>
     *   <li>전체 테이블셋 조회
     *   <li>테이블셋 번호 응답 변환
     * </ol>
     */
    @Override
    @Log("문제셋 목록 조회")
    public List<ProblemSetSummaryOutput> execute() {
        return problemSearchService.findAllProblemSets().stream()
                .map(ProblemSet::getProblemSetId)
                .distinct()
                .sorted()
                .map(ProblemSetSummaryOutput::new)
                .toList();
    }
}
