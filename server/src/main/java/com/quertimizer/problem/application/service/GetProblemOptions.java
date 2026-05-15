package com.quertimizer.problem.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.problem.application.port.in.GetProblemOptionsUseCase;
import com.quertimizer.problem.application.output.AdminProblemOptionOutput;
import com.quertimizer.problem.application.port.out.ProblemRepositoryPort;
import com.quertimizer.problem.domain.entity.Problem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class GetProblemOptions implements GetProblemOptionsUseCase {

    private final ProblemRepositoryPort problemRepository;
    private final ProblemService problemService;

    /**
     * 문제 관리용 문제 옵션 목록을 조회한다.
     *
     * <ol>
     *   <li>문제 테이블셋 번호 정규화
     *   <li>문제 옵션 조회
     * </ol>
     *
     * @param problemSetId 옵션을 조회할 문제 테이블셋 번호
     */
    @Override
    public List<AdminProblemOptionOutput> execute(String problemSetId) {
        String scopedProblemSetId = problemService.normalizeScopedProblemSetId(problemSetId, null);
        List<Problem> problems = problemRepository.findAllByProblemSetIdOrderByProblemIdAsc(scopedProblemSetId);

        return toAdminProblemOptions(problems);
    }

    private List<AdminProblemOptionOutput> toAdminProblemOptions(List<Problem> problems) {
        // 문제 엔티티 목록을 관리자 옵션 응답으로 변환
        return problems.stream()
                .map(problem -> new AdminProblemOptionOutput(problem.getProblemId()))
                .toList();
    }
}
