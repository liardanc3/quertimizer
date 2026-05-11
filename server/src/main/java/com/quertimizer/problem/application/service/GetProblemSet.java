package com.quertimizer.problem.application.service;

import com.quertimizer.problem.application.port.in.GetProblemSetUseCase;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.output.ProblemSetDetailOutput;
import com.quertimizer.problem.application.port.out.ProblemSetRepositoryPort;
import com.quertimizer.problem.application.service.ProblemService;
import com.quertimizer.problem.domain.entity.ProblemSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetProblemSet implements GetProblemSetUseCase {

    private final ProblemSetRepositoryPort problemSetRepository;
    private final ProblemService problemService;

    /**
     * 문제 테이블셋 상세를 조회한다.
     *
     * <ol>
     *   <li>문제 테이블셋 번호 정규화
     *   <li>테이블셋 상세 조회와 응답 생성
     * </ol>
     *
     * @param problemSetId 조회할 문제 테이블셋 번호
     */
    @Override
    public Optional<ProblemSetDetailOutput> execute(String problemSetId) {
        String normalizedProblemSetId = problemService.normalizeProblemSetId(problemSetId);

        if (DbmsType.isScopedProblemSetId(normalizedProblemSetId)) {
            return problemSetRepository.findByProblemSetId(normalizedProblemSetId)
                    .map(this::createProblemSetDetail);
        }

        String postgresqlProblemSetId = problemService.createProblemSetId(DbmsType.POSTGRESQL, normalizedProblemSetId);
        String mysqlProblemSetId = problemService.createProblemSetId(DbmsType.MYSQL, normalizedProblemSetId);
        return problemSetRepository.findByProblemSetId(postgresqlProblemSetId)
                .or(() -> problemSetRepository.findByProblemSetId(mysqlProblemSetId))
                .map(this::createProblemSetDetail);
    }

    private ProblemSetDetailOutput createProblemSetDetail(ProblemSet problemSet) {
        // 문제 테이블셋 상세 응답 생성
        return new ProblemSetDetailOutput(
                problemSet.getProblemSetId(), problemService.normalizeOptionalText(problemSet.getDdl()),
                problemService.normalizeOptionalText(problemSet.getData())
        );
    }
}
