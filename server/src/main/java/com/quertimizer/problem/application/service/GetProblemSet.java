package com.quertimizer.problem.application.service;

import com.quertimizer.problem.application.port.in.GetProblemSetUseCase;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.output.ProblemSetDetailOutput;
import com.quertimizer.problem.application.port.out.ProblemSetRepositoryPort;
import com.quertimizer.problem.application.service.ProblemService;
import com.quertimizer.problem.domain.entity.ProblemSet;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
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
     *   <li>스코프 테이블셋 상세 조회
     *   <li>기준 번호 테이블셋 그룹 조회와 상세 응답 생성
     * </ol>
     *
     * @param problemSetId 조회할 문제 테이블셋 번호
     */
    @Override
    public Optional<ProblemSetDetailOutput> execute(String problemSetId) {
        String normalizedProblemSetId = problemService.normalizeProblemSetId(problemSetId);

        if (DbmsType.isScopedProblemSetId(normalizedProblemSetId)) {
            return problemSetRepository.findByProblemSetId(normalizedProblemSetId)
                    .map(this::createScopedProblemSetDetail);
        }

        ProblemSetGroup problemSetGroup = findProblemSetGroup(normalizedProblemSetId);

        if (problemSetGroup.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(new ProblemSetDetailOutput(
                normalizedProblemSetId, problemSetGroup.getDdl(DbmsType.POSTGRESQL),
                problemSetGroup.getDdl(DbmsType.MYSQL), problemSetGroup.getData(DbmsType.POSTGRESQL),
                problemSetGroup.getData(DbmsType.MYSQL)
        ));
    }

    private ProblemSetDetailOutput createScopedProblemSetDetail(ProblemSet problemSet) {
        // MySQL 문제 테이블셋 상세 응답 생성
        if (problemSet.getDbmsType() == DbmsType.MYSQL) {
            return new ProblemSetDetailOutput(
                    problemSet.getProblemSetId(), "",
                    problemService.normalizeOptionalText(problemSet.getDdl()), "",
                    problemService.normalizeOptionalText(problemSet.getData())
            );
        }

        // PostgreSQL 문제 테이블셋 상세 응답 생성
        return new ProblemSetDetailOutput(
                problemSet.getProblemSetId(), problemService.normalizeOptionalText(problemSet.getDdl()),
                "", problemService.normalizeOptionalText(problemSet.getData()), ""
        );
    }

    private ProblemSetGroup findProblemSetGroup(String problemSetId) {
        // DBMS별 문제 테이블셋 조회
        Map<DbmsType, ProblemSet> problemSetsByDbms = new EnumMap<>(DbmsType.class);
        for (DbmsType dbmsType : DbmsType.values()) {
            problemSetRepository.findByProblemSetId(problemService.createProblemSetId(dbmsType, problemSetId))
                    .ifPresent(problemSet -> problemSetsByDbms.put(dbmsType, problemSet));
        }

        return new ProblemSetGroup(problemSetsByDbms);
    }

    private static final class ProblemSetGroup {
        // DBMS별 문제 테이블셋 묶음

        private final Map<DbmsType, ProblemSet> problemSetsByDbms;

        private ProblemSetGroup(Map<DbmsType, ProblemSet> problemSetsByDbms) {
            this.problemSetsByDbms = problemSetsByDbms;
        }

        private boolean isEmpty() {
            // 비어 있는지 확인
            return problemSetsByDbms.isEmpty();
        }

        private String getDdl(DbmsType dbmsType) {
            // DBMS별 DDL 조회
            ProblemSet problemSet = problemSetsByDbms.get(dbmsType);

            // DDL null 여부에 따른 빈 문자열 대체 반환
            return problemSet != null && problemSet.getDdl() != null ? problemSet.getDdl().trim() : "";
        }

        private String getData(DbmsType dbmsType) {
            // DBMS별 데이터 SQL 조회
            ProblemSet problemSet = problemSetsByDbms.get(dbmsType);

            // 데이터 SQL null 여부에 따른 빈 문자열 대체 반환
            return problemSet != null && problemSet.getData() != null ? problemSet.getData().trim() : "";
        }
    }
}
