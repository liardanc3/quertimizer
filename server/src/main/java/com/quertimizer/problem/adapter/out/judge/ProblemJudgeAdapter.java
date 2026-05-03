package com.quertimizer.problem.adapter.out.judge;

import com.quertimizer.judge.application.input.AnalyzeJudgeEnvironmentInput;
import com.quertimizer.judge.application.input.CreateDataset;
import com.quertimizer.judge.application.input.CreateJudgeEnvironmentInput;
import com.quertimizer.judge.application.input.CreateSqlExecutionHashInput;
import com.quertimizer.judge.application.input.ExecuteIsolatedJudgeSqlInput;
import com.quertimizer.judge.application.input.ExecuteJudgeSqlInput;
import com.quertimizer.judge.application.output.ExecutionMode;
import com.quertimizer.judge.application.output.SqlExecutionResult;
import com.quertimizer.judge.application.port.in.AnalyzeJudgeEnvironmentUseCase;
import com.quertimizer.judge.application.port.in.CancelJudgeExecutionUseCase;
import com.quertimizer.judge.application.port.in.CreateJudgeDatasetUseCase;
import com.quertimizer.judge.application.port.in.CreateJudgeEnvironmentUseCase;
import com.quertimizer.judge.application.port.in.CreateSqlExecutionHashUseCase;
import com.quertimizer.judge.application.port.in.DeleteJudgeDatasetUseCase;
import com.quertimizer.judge.application.port.in.DropJudgeEnvironmentUseCase;
import com.quertimizer.judge.application.port.in.ExecuteIsolatedJudgeSqlUseCase;
import com.quertimizer.judge.application.port.in.ExecuteJudgeSqlUseCase;
import com.quertimizer.judge.application.port.in.HasJudgeDatasetUseCase;
import com.quertimizer.judge.application.port.in.ParseJudgeSqlStatementsUseCase;
import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;
import com.quertimizer.judge.domain.entity.JudgeExecutionId;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.domain.model.EnvironmentPolicy;
import com.quertimizer.judge.domain.model.ExecutionOptions;
import com.quertimizer.judge.domain.model.IsolationPolicy;
import com.quertimizer.problem.application.output.ProblemJudgeExecutionResult;
import com.quertimizer.problem.application.output.ProblemJudgeSqlStatement;
import com.quertimizer.problem.application.port.out.ProblemJudgePort;
import com.quertimizer.problem.domain.model.ProblemJudgeExecutionMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ProblemJudgeAdapter implements ProblemJudgePort {

    private final CreateJudgeDatasetUseCase createJudgeDataset;
    private final HasJudgeDatasetUseCase hasJudgeDataset;
    private final DeleteJudgeDatasetUseCase deleteJudgeDataset;
    private final CreateJudgeEnvironmentUseCase createJudgeEnvironment;
    private final DropJudgeEnvironmentUseCase dropJudgeEnvironment;
    private final ExecuteJudgeSqlUseCase executeJudgeSql;
    private final ExecuteIsolatedJudgeSqlUseCase executeIsolatedJudgeSql;
    private final AnalyzeJudgeEnvironmentUseCase analyzeJudgeEnvironment;
    private final CreateSqlExecutionHashUseCase createSqlExecutionHash;
    private final CancelJudgeExecutionUseCase cancelJudgeExecution;
    private final ParseJudgeSqlStatementsUseCase parseJudgeSqlStatements;

    @Override
    public String createDataset(DbmsType dbmsType, String ddl, String dataSql) {
        // problem SQL 자료를 judge 데이터셋으로 등록
        return createJudgeDataset.execute(new CreateDataset(dbmsType, ddl, dataSql, List.of())).getValue();
    }

    @Override
    public boolean hasDataset(String datasetId) {
        // judge 데이터셋 존재 여부 확인
        return hasJudgeDataset.execute(datasetId);
    }

    @Override
    public void deleteDataset(String datasetId) {
        // judge 데이터셋 제거
        deleteJudgeDataset.execute(new JudgeDatasetId(datasetId));
    }

    @Override
    public String createAnswerHash(String datasetId, String answerSql) {
        // judge 데이터셋 기준 정답 SQL 실행 해시 생성
        return createSqlExecutionHash.execute(new CreateSqlExecutionHashInput(
                new JudgeDatasetId(datasetId), answerSql, ExecutionOptions.officialCost()
        )).getResultHash();
    }

    @Override
    public String createInteractiveEnvironment(String datasetId) {
        // 인터랙티브 문제 실행 환경 생성
        return createJudgeEnvironment.execute(new CreateJudgeEnvironmentInput(
                new JudgeDatasetId(datasetId), EnvironmentPolicy.interactive()
        )).getValue();
    }

    @Override
    public String createSubmissionEnvironment(String datasetId) {
        // 공식 제출용 문제 실행 환경 생성
        return createJudgeEnvironment.execute(new CreateJudgeEnvironmentInput(
                new JudgeDatasetId(datasetId), new EnvironmentPolicy(true, true, false)
        )).getValue();
    }

    @Override
    public ProblemJudgeExecutionResult executeInteractiveSql(String executionId, String environmentId,
                                                             String sql, int page, int pageSize) {
        // 인터랙티브 실행 옵션으로 SQL 실행
        return toProblemResult(executeJudgeSql.execute(new ExecuteJudgeSqlInput(
                new JudgeExecutionId(executionId), new JudgeEnvironmentId(environmentId),
                sql, new ExecutionOptions(60, page, pageSize, true, false)
        )));
    }

    @Override
    public ProblemJudgeExecutionResult executeOfficialSql(String executionId, String environmentId, String sql) {
        // 공식 비용 측정 옵션으로 SQL 실행
        return toProblemResult(executeJudgeSql.execute(new ExecuteJudgeSqlInput(
                new JudgeExecutionId(executionId), new JudgeEnvironmentId(environmentId),
                sql, ExecutionOptions.officialCost()
        )));
    }

    @Override
    public ProblemJudgeExecutionResult executeSubmissionAnswerSql(String executionId, String environmentId, String sql) {
        // 제출 정답 비교 옵션으로 SQL 실행
        return toProblemResult(executeJudgeSql.execute(new ExecuteJudgeSqlInput(
                new JudgeExecutionId(executionId), new JudgeEnvironmentId(environmentId),
                sql, ExecutionOptions.submissionAnswer()
        )));
    }

    @Override
    public ProblemJudgeExecutionResult analyzeOfficialEnvironment(String executionId, String environmentId) {
        // 공식 비용 측정 옵션으로 실행 환경 통계 갱신
        return toProblemResult(analyzeJudgeEnvironment.execute(new AnalyzeJudgeEnvironmentInput(
                new JudgeExecutionId(executionId), new JudgeEnvironmentId(environmentId), ExecutionOptions.officialCost()
        )));
    }

    @Override
    public ProblemJudgeExecutionResult executeIsolatedOfficialSql(String executionId, String datasetId, String sql) {
        // 임시 격리 환경에서 공식 비용 옵션으로 SQL 실행
        return toProblemResult(executeIsolatedJudgeSql.execute(new ExecuteIsolatedJudgeSqlInput(
                new JudgeExecutionId(executionId), new JudgeDatasetId(datasetId),
                List.of(), sql, IsolationPolicy.cleanRoom(), ExecutionOptions.officialCost()
        )));
    }

    @Override
    public void dropEnvironment(String environmentId) {
        // judge 실행 환경 제거
        dropJudgeEnvironment.execute(new JudgeEnvironmentId(environmentId));
    }

    @Override
    public void cancelExecution(String executionId) {
        // 진행 중인 judge 실행 취소
        cancelJudgeExecution.execute(new JudgeExecutionId(executionId));
    }

    @Override
    public List<ProblemJudgeSqlStatement> parseStatements(String sql) {
        // judge SQL 파서 결과를 problem 전용 문장 모델로 변환
        return parseJudgeSqlStatements.execute(sql).stream()
                .map(statement -> new ProblemJudgeSqlStatement(statement.getSql(), toProblemMode(statement.getMode())))
                .toList();
    }

    private ProblemJudgeExecutionResult toProblemResult(SqlExecutionResult result) {
        // judge 실행 결과를 problem 포트 출력 모델로 변환
        return new ProblemJudgeExecutionResult(
                toProblemMode(result.getMode()),
                result.getColumns(), result.getRows(),
                result.getRowCount(), result.getCurrentPage(), result.getPageSize(),
                result.getExecutionTimeMs(), result.getCost(), result.getPlanLines()
        );
    }

    private ProblemJudgeExecutionMode toProblemMode(ExecutionMode mode) {
        // judge 실행 모드를 problem 전용 실행 모드로 변환
        return switch (mode) {
            case SELECT -> ProblemJudgeExecutionMode.SELECT;
            case EXPLAIN -> ProblemJudgeExecutionMode.EXPLAIN;
            case EXPLAIN_ANALYZE -> ProblemJudgeExecutionMode.EXPLAIN_ANALYZE;
            case ANALYZE -> ProblemJudgeExecutionMode.ANALYZE;
            case INDEX_COMMAND -> ProblemJudgeExecutionMode.INDEX_COMMAND;
            case COMMAND -> ProblemJudgeExecutionMode.COMMAND;
        };
    }
}
