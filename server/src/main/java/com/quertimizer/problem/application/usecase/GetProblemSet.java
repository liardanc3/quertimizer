package com.quertimizer.problem.application.usecase;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.problem.application.input.ProblemSetAccessInput;
import com.quertimizer.problem.application.output.ProblemSetDetailOutput;
import com.quertimizer.problem.application.port.ProblemSetRepository;
import com.quertimizer.problem.application.service.ProblemService;
import com.quertimizer.problem.domain.entity.ProblemSet;
import com.quertimizer.problem.domain.policy.ProblemManagementPolicy;
import com.quertimizer.user.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class GetProblemSet {

    private final ProblemSetRepository problemSetRepository;
    private final ProblemService problemService;
    private final ProblemManagementPolicy problemManagementPolicy;

    /**
     * 문제 테이블셋 상세를 조회한다.
     *
     * <ol>
     *   <li>문제 관리 사용자 권한 확인
     *   <li>문제 테이블셋 번호 정규화
     *   <li>스코프 테이블셋 접근 검증과 상세 조회
     *   <li>기준 번호 테이블셋 그룹 조회와 상세 응답 생성
     * </ol>
     *
     * @param input 조회할 문제 테이블셋과 인증 이메일 입력
     */
    public Optional<ProblemSetDetailOutput> execute(ProblemSetAccessInput input) {
        User currentUser = problemService.requireProblemManagementUser(input.getAuthenticatedEmail());

        String normalizedProblemSetId = problemService.normalizeProblemSetId(input.getProblemSetId());

        if (problemManagementPolicy.isScopedProblemSetId(normalizedProblemSetId)) {
            problemService.validateProblemSetAccess(currentUser, normalizedProblemSetId);
            return problemSetRepository.findById(normalizedProblemSetId)
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
            problemSetRepository.findById(problemService.createProblemSetId(dbmsType, problemSetId))
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
