package com.quertimizer.judge.application.service;

import com.quertimizer.global.log.Log;
import com.quertimizer.judge.application.input.AnalyzeEnvironmentInput;
import com.quertimizer.judge.application.input.CreateDatasetInput;
import com.quertimizer.judge.application.input.CreateEnvironmentInput;
import com.quertimizer.judge.application.input.CreateSqlExecutionHashInput;
import com.quertimizer.judge.application.input.ExecuteIsolatedSqlInput;
import com.quertimizer.judge.application.input.ExecuteSqlInput;
import com.quertimizer.judge.application.model.SqlExecutorTicket;
import com.quertimizer.judge.application.output.SqlExecutionHashResult;
import com.quertimizer.judge.application.output.SqlExecutionResult;
import com.quertimizer.judge.application.output.SqlStatement;
import com.quertimizer.judge.application.port.in.JudgeApplicationPort;
import com.quertimizer.judge.application.port.out.DefinitionRepositoryPort;
import com.quertimizer.judge.application.port.out.DatabaseSnapshotPort;
import com.quertimizer.judge.application.port.out.TemplateRepositoryPort;
import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.DatasetTemplateDefinition;
import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;
import com.quertimizer.judge.domain.entity.JudgeExecutionId;
import com.quertimizer.judge.domain.entity.JudgeSetupSqlId;
import com.quertimizer.judge.domain.entity.SetupSqlDefinition;
import com.quertimizer.judge.domain.model.ExecutionMode;
import com.quertimizer.judge.domain.model.IsolationPolicy;
import com.quertimizer.judge.domain.model.QueuePriority;
import com.quertimizer.judge.domain.model.QueueStatusListener;
import com.quertimizer.judge.domain.model.DatabaseSnapshot;
import com.quertimizer.judge.domain.policy.SqlDefinitionPolicy;
import com.quertimizer.judge.domain.policy.SqlExecutionPolicy;
import com.quertimizer.judge.domain.service.SqlResultHashSupport;
import com.quertimizer.judge.domain.service.SqlStatementParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

import static com.quertimizer.judge.domain.model.JudgeFailReason.SETUP_SQL_DATASET_MISMATCH;
import static com.quertimizer.judge.domain.model.JudgeFailReason.UNKNOWN_DATASET_ID;
import static com.quertimizer.judge.domain.model.JudgeFailReason.UNKNOWN_ENVIRONMENT_ID;
import static com.quertimizer.judge.domain.model.JudgeFailReason.UNKNOWN_SETUP_SQL_ID;
import static com.quertimizer.judge.domain.model.SqlPolicyFailReason.SINGLE_SQL_ONLY;

@Component
@RequiredArgsConstructor
public class JudgeApplicationService implements JudgeApplicationPort {

    private final SqlDefinitionPolicy definitionPolicy;
    private final SqlExecutionPolicy executionPolicy;
    private final SqlStatementParser statementParser;
    private final DefinitionRepositoryPort definitionRepository;
    private final TemplateRepositoryPort templateRepository;
    private final SqlExecutorPool sqlExecutorPool;
    private final DatabaseSnapshotPort databaseSnapshotPort;

    @Override
    @Log("데이터셋 생성")
    public JudgeDatasetId createDataset(CreateDatasetInput input) {
        // DDL과 데이터 SQL 검증 후 데이터셋 정의 생성
        definitionPolicy.validateDdl(input.getDdl());
        definitionPolicy.validateDataSql(input.getDataSql());
        DatasetDefinition datasetDefinition = new DatasetDefinition(
                null, input.getDbmsType(), input.getDdl(),
                input.getDataSql(), input.getBaseIndexDdls()
        );

        // 데이터셋 정의와 물리 템플릿 저장, 실패 시 생성 리소스 정리
        JudgeDatasetId datasetId = null;
        DatasetTemplateDefinition templateDefinition = null;
        try {
            datasetDefinition = definitionRepository.saveDataset(datasetDefinition, input.isStoreSqlDefinition());
            datasetId = datasetDefinition.getDatasetId();
            SqlExecutorTicket ticket = sqlExecutorPool.requestExecutor(
                    input.getDbmsType(), input.getQueuePriority(), QueueStatusListener.noop()
            );
            try (SqlExecutor executor = sqlExecutorPool.acquireExecutor(ticket)) {
                templateDefinition = executor.createDataset(datasetDefinition);
            }
            if (templateDefinition != null) {
                templateRepository.saveDatasetTemplate(templateDefinition);
            }
            return datasetId;
        } catch (RuntimeException exception) {
            cleanupFailedDataset(datasetId, templateDefinition);
            throw exception;
        }
    }

    @Override
    @Log("데이터셋 존재 확인")
    public boolean hasDataset(Long datasetId) {
        // 데이터셋 정의 존재 여부 반환
        return definitionRepository.findDataset(new JudgeDatasetId(datasetId)).isPresent();
    }

    @Override
    @Log("데이터셋 삭제")
    public void deleteDataset(JudgeDatasetId datasetId) {
        // 물리 템플릿 제거 후 데이터셋 정의 제거
        templateRepository.findDatasetTemplate(datasetId).ifPresent(sqlExecutorPool::dropDataset);
        definitionRepository.deleteDataset(datasetId);
    }

    @Override
    @Log("실행 환경 생성")
    public JudgeEnvironmentId createEnvironment(CreateEnvironmentInput input) {
        // 데이터셋 정의 조회와 실행 환경 ID 생성
        DatasetDefinition dataset = requireDataset(input.getDatasetId());
        JudgeEnvironmentId environmentId = new JudgeEnvironmentId("environment-" + UUID.randomUUID());
        SqlExecutorTicket ticket = sqlExecutorPool.requestExecutor(
                dataset.getDbmsType(), input.getQueuePriority(), input.getQueueStatusListener()
        );

        // 대기열 ticket 기준 SQL 실행기 할당 후 실행 환경 생성
        try (SqlExecutor executor = sqlExecutorPool.acquireExecutor(ticket)) {
            return executor.createEnvironment(environmentId, dataset, input.getPolicy());
        }
    }

    @Override
    @Log("실행 환경 삭제")
    public void dropEnvironment(JudgeEnvironmentId environmentId) {
        // 등록되지 않은 실행 환경 제거 요청 무시
        if (!sqlExecutorPool.hasEnvironment(environmentId)) {
            return;
        }

        // 등록된 실행 환경 제거
        sqlExecutorPool.dropEnvironment(environmentId);
    }

    @Override
    @Log("SQL 실행")
    public SqlExecutionResult executeSql(ExecuteSqlInput input) {
        // 실행 모드와 실행 SQL 결정
        ExecutionMode mode = input.getOptions().isValidateSql()
                ? executionPolicy.resolveMode(input.getSql())
                : ExecutionMode.SELECT;
        String sql = input.getOptions().isValidateSql() ? resolveSingleStatement(input.getSql()) : input.getSql();

        // 영속 SQL 실행기 ticket 발급 후 SQL 실행
        SqlExecutorTicket ticket = sqlExecutorPool.requestExecutor(
                input.getEnvironmentId(), QueuePriority.NORMAL, QueueStatusListener.noop()
        );
        try (SqlExecutor executor = sqlExecutorPool.acquireExecutor(ticket)) {
            return executor.execute(input, sql, mode);
        }
    }

    @Override
    @Log("SQL 전체 실행")
    public SqlExecutionResult executeSelectAllSql(ExecuteSqlInput input) {
        // 읽기 전용 단일 SQL 검증과 실행 SQL 결정
        definitionPolicy.validateReadOnlySql(input.getSql());
        String sql = resolveSingleStatement(input.getSql());

        // 영속 SQL 실행기 ticket 발급 후 SELECT 전체 결과 조회
        SqlExecutorTicket ticket = sqlExecutorPool.requestExecutor(
                input.getEnvironmentId(), QueuePriority.NORMAL, QueueStatusListener.noop()
        );
        try (SqlExecutor executor = sqlExecutorPool.acquireExecutor(ticket)) {
            return executor.executeSelectAll(input, sql);
        }
    }

    @Override
    @Log("실행 환경 통계 갱신")
    public SqlExecutionResult analyzeEnvironment(AnalyzeEnvironmentInput input) {
        // 실행 환경 존재 여부 확인
        if (!sqlExecutorPool.hasEnvironment(input.getEnvironmentId())) {
            throw new IllegalArgumentException(UNKNOWN_ENVIRONMENT_ID.format(input.getEnvironmentId()));
        }

        // 영속 SQL 실행기 ticket 발급 후 통계 갱신
        SqlExecutorTicket ticket = sqlExecutorPool.requestExecutor(
                input.getEnvironmentId(), QueuePriority.NORMAL, QueueStatusListener.noop()
        );
        try (SqlExecutor executor = sqlExecutorPool.acquireExecutor(ticket)) {
            return executor.analyze(input);
        }
    }

    @Override
    @Log("정답 기준 생성")
    public SqlExecutionHashResult createSqlExecutionHash(CreateSqlExecutionHashInput input) {
        // 읽기 전용 SQL 검증 후 격리 SQL 실행
        definitionPolicy.validateReadOnlySql(input.getSql());
        SqlExecutionResult executionResult = executeIsolatedSql(new ExecuteIsolatedSqlInput(
                new JudgeExecutionId("hash-" + UUID.randomUUID()), input.getDatasetId(), List.of(),
                input.getSql(), IsolationPolicy.cleanRoom(), input.getOptions()
        ));

        // 실행 결과 기준 해시 생성
        String resultHash = SqlResultHashSupport.hashResult(executionResult.getColumns(), executionResult.getRows());
        return new SqlExecutionHashResult(resultHash, executionResult);
    }

    @Override
    @Log("SQL 실행 취소")
    public void cancelExecution(JudgeExecutionId executionId) {
        // 활성 실행이 아니면 취소 요청 무시
        if (!sqlExecutorPool.hasActiveExecution(executionId)) {
            return;
        }

        // 실행 ID 기준 SQL 실행 취소
        sqlExecutorPool.cancel(executionId);
    }

    @Override
    @Log("SQL 구문 파싱")
    public List<SqlStatement> parseSqlStatements(String sql) {
        // SQL 문장 분리와 실행 모드 분류
        return statementParser.splitStatements(sql).stream()
                .map(statementSql -> new SqlStatement(statementSql, executionPolicy.resolveMode(statementSql)))
                .toList();
    }

    @Override
    public DatabaseSnapshot createDatabaseSnapshot() {
        // DB 실행 환경 현재 snapshot 조회
        return databaseSnapshotPort.createSnapshot();
    }

    private SqlExecutionResult executeIsolatedSql(ExecuteIsolatedSqlInput input) {
        // 읽기 전용 SQL 검증과 데이터셋, 설정 SQL 정의 조회
        definitionPolicy.validateReadOnlySql(input.getTargetSql());
        DatasetDefinition dataset = requireDataset(input.getDatasetId());
        List<SetupSqlDefinition> setupSqlDefinitions = input.getSetupSqlIds().stream()
                .map(setupSqlId -> requireSetupSql(input, setupSqlId))
                .toList();

        // 격리 SQL 실행기 ticket 발급 후 설정 SQL, 통계 갱신, 대상 SQL 실행
        SqlExecutorTicket ticket = sqlExecutorPool.requestExecutor(dataset.getDbmsType(), QueuePriority.NORMAL, QueueStatusListener.noop());
        try (SqlExecutor executor = sqlExecutorPool.acquireExecutor(ticket)) {
            return executor.executeIsolated(input, dataset, setupSqlDefinitions);
        }
    }

    private DatasetDefinition requireDataset(JudgeDatasetId datasetId) {
        // 데이터셋 정의 필수 조회
        return definitionRepository.findDataset(datasetId)
                .orElseThrow(() -> new IllegalArgumentException(UNKNOWN_DATASET_ID.format(datasetId)));
    }

    private SetupSqlDefinition requireSetupSql(ExecuteIsolatedSqlInput input, JudgeSetupSqlId setupSqlId) {
        // 설정 SQL 정의 조회와 데이터셋 일치 여부 검증
        SetupSqlDefinition setupSqlDefinition = definitionRepository.findSetupSql(setupSqlId)
                .orElseThrow(() -> new IllegalArgumentException(UNKNOWN_SETUP_SQL_ID.format(setupSqlId)));
        if (!setupSqlDefinition.getDatasetId().equals(input.getDatasetId())) {
            throw new IllegalArgumentException(SETUP_SQL_DATASET_MISMATCH.format(setupSqlId));
        }
        return setupSqlDefinition;
    }

    private String resolveSingleStatement(String sql) {
        // 단일 SQL 문장 추출
        List<String> statements = statementParser.splitStatements(sql);
        if (statements.size() != 1) {
            throw new IllegalArgumentException(SINGLE_SQL_ONLY.getMessage());
        }

        return statements.get(0);
    }

    private void cleanupFailedDataset(JudgeDatasetId datasetId, DatasetTemplateDefinition templateDefinition) {
        if (datasetId == null) {
            return;
        }

        // 데이터셋 생성 실패 시 물리 템플릿과 저장된 정의 제거
        if (templateDefinition != null) {
            sqlExecutorPool.dropDataset(templateDefinition);
        } else {
            templateRepository.findDatasetTemplate(datasetId).ifPresent(sqlExecutorPool::dropDataset);
        }

        // 저장된 데이터셋 정의 제거
        definitionRepository.deleteDataset(datasetId);
    }
}
