package com.quertimizer.problem.application.usecase;

import com.quertimizer.problem.application.output.ProblemDetailOutput;
import com.quertimizer.problem.application.service.ProblemService;
import com.quertimizer.problem.application.store.ProblemStore;
import com.quertimizer.problem.domain.entity.Problem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetProblem {

    private final ProblemStore problemStore;
    private final ProblemService problemService;

    /**
     * 문제 상세를 조회한다.
     *
     * <ol>
     *   <li>문제 조회
     *   <li>문제 테이블셋 조회와 상세 응답 변환
     * </ol>
     *
     * @param problemId 조회할 문제 번호
     */
    public Optional<ProblemDetailOutput> execute(String problemId) {
        Optional<Problem> problem = problemStore.findProblem(problemId);

        return problem.map(foundProblem -> problemStore.findProblemSet(foundProblem.getResolvedProblemSetId())
                .map(problemSet -> problemService.toProblemDetailOutput(foundProblem, problemSet))
                .orElseGet(() -> problemService.toProblemDetailOutput(foundProblem)));
    }
}
