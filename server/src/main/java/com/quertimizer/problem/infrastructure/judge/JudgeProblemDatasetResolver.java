package com.quertimizer.problem.infrastructure.judge;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.global.exception.BusinessException;
import com.quertimizer.judge.application.input.CreateJudgeDatasetInput;
import com.quertimizer.judge.application.port.JudgeDefinitionStore;
import com.quertimizer.judge.application.port.JudgePort;
import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import com.quertimizer.problem.application.port.ProblemSetRepository;
import com.quertimizer.problem.application.store.ProblemStore;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.domain.entity.ProblemSet;
import com.quertimizer.problem.domain.model.ProblemQueryFailReason;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.quertimizer.problem.domain.model.ProblemManagementFailReason.PROBLEM_SET_NOT_FOUND;

@Component
@RequiredArgsConstructor
public class JudgeProblemDatasetResolver {

    private final ProblemStore problemStore;
    private final ProblemSetRepository problemSetRepository;
    private final JudgePort judgePort;
    private final JudgeDefinitionStore judgeDefinitionStore;

    @Transactional
    public ResolvedProblemDataset resolve(String problemId, DbmsType requestedDbmsType) {
        // 문제와 문제셋 조회 후 실행 대상 DBMS와 데이터셋 원본 SQL 확정
        Problem problem = problemStore.findProblem(problemId)
                .orElseThrow(() -> new BusinessException(ProblemQueryFailReason.PROBLEM_INFO_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        ProblemSet problemSet = problemStore.findProblemSet(problem.getResolvedProblemSetId())
                .orElseThrow(() -> new BusinessException(PROBLEM_SET_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND));
        DbmsType dbmsType = problem.getDbmsType() != null ? problem.getDbmsType() : requestedDbmsType;

        // 저장된 데이터셋 키의 judge 정의 저장소 존재 시 재사용
        String storedDatasetId = normalize(problemSet.getJudgeDatasetId());
        if (!storedDatasetId.isBlank() && judgeDefinitionStore.findDataset(new JudgeDatasetId(storedDatasetId)).isPresent()) {
            return new ResolvedProblemDataset(problem, problemSet, storedDatasetId, dbmsType);
        }

        // 키 누락 또는 정의 누락 시 문제셋 원본 SQL 기반 새 데이터셋 생성과 문제셋 반영
        JudgeDatasetId datasetId = judgePort.createDataset(new CreateJudgeDatasetInput(
                toJudgeDbmsType(dbmsType), problemSet.getDdl(),
                problemSet.getActualDataSql(), List.of()
        ));
        problemSet.changeContent(
                problemSet.getDdl(),
                problemSet.getActualDataSql(),
                problemSet.getTemplateVersion(),
                dbmsType,
                datasetId.getValue()
        );
        problemSetRepository.save(problemSet);
        problemStore.loadProblems();

        return new ResolvedProblemDataset(problem, problemSet, datasetId.getValue(), dbmsType);
    }

    private String normalize(String value) {
        // null 문자열 빈 문자열 정리
        return value != null ? value.trim() : "";
    }

    private com.quertimizer.judge.domain.model.DbmsType toJudgeDbmsType(DbmsType dbmsType) {
        // 문제 DBMS 유형을 judge DBMS 유형으로 변환
        return switch (dbmsType) {
            case POSTGRESQL -> com.quertimizer.judge.domain.model.DbmsType.POSTGRESQL;
            case MYSQL -> com.quertimizer.judge.domain.model.DbmsType.MYSQL;
        };
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
            return problem;
        }

        public ProblemSet getProblemSet() {
            return problemSet;
        }

        public String getDatasetId() {
            return datasetId;
        }

        public DbmsType getDbmsType() {
            return dbmsType;
        }
    }
}
