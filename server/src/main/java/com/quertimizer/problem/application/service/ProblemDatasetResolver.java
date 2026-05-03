package com.quertimizer.problem.application.service;

import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.problem.application.port.out.ProblemJudgePort;
import com.quertimizer.problem.application.port.out.ProblemRepositoryPort;
import com.quertimizer.problem.application.port.out.ProblemSetRepositoryPort;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.domain.entity.ProblemSet;
import com.quertimizer.problem.domain.model.ProblemQueryFailReason;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_SET_NOT_FOUND;

@Service
@RequiredArgsConstructor
public class ProblemDatasetResolver {

    private final ProblemRepositoryPort problemRepository;
    private final ProblemSetRepositoryPort problemSetRepository;
    private final ProblemJudgePort problemJudgePort;

    @Transactional
    public ResolvedProblemDataset resolve(String problemId, DbmsType requestedDbmsType) {
        // 문제와 문제셋 조회 후 실행 대상 DBMS와 데이터셋 원본 SQL 확정
        Problem problem = problemRepository.findByProblemId(problemId)
                .orElseThrow(() -> new BusinessException(ProblemQueryFailReason.PROBLEM_INFO_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        ProblemSet problemSet = problemSetRepository.findByProblemSetId(problem.getResolvedProblemSetId())
                .orElseThrow(() -> new BusinessException(PROBLEM_SET_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        DbmsType dbmsType = problem.getDbmsType() != null ? problem.getDbmsType() : requestedDbmsType;

        // 저장된 데이터셋 키가 judge에 남아 있으면 기존 데이터셋 재사용
        String storedDatasetId = normalize(problemSet.getDatasetId());
        if (!storedDatasetId.isBlank() && problemJudgePort.hasDataset(storedDatasetId)) {
            return new ResolvedProblemDataset(problem, problemSet, storedDatasetId, dbmsType);
        }

        // 키 누락 또는 judge 데이터셋 누락 시 문제셋 원본 SQL 기반 새 데이터셋 생성
        String datasetId = problemJudgePort.createDataset(dbmsType, problemSet.getDdl(), problemSet.getActualDataSql());
        problemSet.changeContent(problemSet.getDdl(), problemSet.getActualDataSql(), dbmsType, datasetId);
        problemSetRepository.save(problemSet);

        return new ResolvedProblemDataset(problem, problemSet, datasetId, dbmsType);
    }

    private String normalize(String value) {
        // null 문자열 빈 문자열 정리
        return value != null ? value.trim() : "";
    }

    public static class ResolvedProblemDataset {
        private final Problem problem;
        private final ProblemSet problemSet;
        private final String datasetId;
        private final DbmsType dbmsType;

        private ResolvedProblemDataset(Problem problem, ProblemSet problemSet, String datasetId, DbmsType dbmsType) {
            this.problem = problem;
            this.problemSet = problemSet;
            this.datasetId = datasetId;
            this.dbmsType = dbmsType;
        }

    public Problem getProblem() {
            // 문제 엔티티 반환
            return problem;
        }

    public ProblemSet getProblemSet() {
            // 문제셋 엔티티 반환
            return problemSet;
        }

    public String getDatasetId() {
            // judge 데이터셋 ID 반환
            return datasetId;
        }

    public DbmsType getDbmsType() {
            // 실행 대상 DBMS 유형 반환
            return dbmsType;
        }
    }
}
