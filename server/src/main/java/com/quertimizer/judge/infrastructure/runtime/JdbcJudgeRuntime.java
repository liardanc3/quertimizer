package com.quertimizer.judge.infrastructure.runtime;

import com.quertimizer.judge.application.port.JudgeRuntime;
import com.quertimizer.judge.application.input.AnalyzeJudgeEnvironmentInput;
import com.quertimizer.judge.application.input.CreateJudgeDatasetInput;
import com.quertimizer.judge.application.input.CreateJudgeEnvironmentInput;
import com.quertimizer.judge.application.input.CreateJudgeReferenceInput;
import com.quertimizer.judge.application.input.CreateJudgeSetupSqlInput;
import com.quertimizer.judge.application.input.ExecuteJudgeSqlInput;
import com.quertimizer.judge.application.input.ExecuteIsolatedJudgeSqlInput;
import com.quertimizer.judge.infrastructure.dialect.JudgeDialect;
import com.quertimizer.judge.infrastructure.dialect.JudgeDialectProvider;
import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.infrastructure.runtime.NoOpJudgeTemplateStore;
import com.quertimizer.judge.domain.entity.ReferenceDefinition;
import com.quertimizer.judge.domain.entity.SetupSqlDefinition;
import com.quertimizer.judge.application.port.JudgeDefinitionStore;
import com.quertimizer.judge.application.port.JudgeTemplateStore;
import com.quertimizer.judge.domain.event.ExecutionAccepted;
import com.quertimizer.judge.domain.event.ExecutionCompleted;
import com.quertimizer.judge.domain.event.ExecutionFailed;
import com.quertimizer.judge.domain.event.JudgeListener;
import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.ids.JudgeEnvironmentId;
import com.quertimizer.judge.domain.entity.ids.JudgeExecutionId;
import com.quertimizer.judge.domain.entity.ids.JudgeReferenceId;
import com.quertimizer.judge.domain.entity.ids.JudgeSetupSqlId;
import com.quertimizer.judge.domain.policy.SqlDefinitionPolicy;
import com.quertimizer.judge.domain.policy.SqlExecutionPolicy;
import com.quertimizer.judge.application.output.ExecutionMode;
import com.quertimizer.judge.application.output.SqlExecutionResult;
import com.quertimizer.judge.application.output.SqlPlanCostParser;
import com.quertimizer.judge.application.output.SqlReferenceResult;
import com.quertimizer.judge.application.output.SqlResultHashSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;

public class JdbcJudgeRuntime implements JudgeRuntime {

    private static final Logger log = LoggerFactory.getLogger(JdbcJudgeRuntime.class);

    private final RuntimeDatabaseCluster databaseCluster;
    private final JudgeDefinitionStore definitionStore;
    private final JudgeDialectProvider dialectProvider;
    private final RuntimeEnvironmentNamingStrategy namingStrategy;
    private final SqlStatementParser statementParser;
    private final SqlDefinitionPolicy definitionPolicy;
    private final SqlExecutionPolicy executionPolicy;
    private final RuntimeEnvironmentProvisioner environmentProvisioner;
    private final JudgeTemplateStore templateStore;
    private final DatasetTemplateProvisioner templateProvisioner;
    private final RuntimeStatisticsInitializer statisticsInitializer;
    private final ConcurrentHashMap<JudgeEnvironmentId, PersistentRuntimeEnvironment> environments = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<JudgeExecutionId, Statement> activeStatements = new ConcurrentHashMap<>();

    public JdbcJudgeRuntime(RuntimeDatabaseCluster databaseCluster, JudgeDefinitionStore definitionStore,
                            JudgeDialectProvider dialectProvider, RuntimeEnvironmentNamingStrategy namingStrategy,
                            SqlStatementParser statementParser, SqlDefinitionPolicy definitionPolicy) {
        this(databaseCluster, definitionStore, dialectProvider, namingStrategy,
                statementParser, definitionPolicy,
                new SqlReplayEnvironmentProvisioner(databaseCluster, dialectProvider, namingStrategy, statementParser),
                new NoOpJudgeTemplateStore(), new NoOpDatasetTemplateProvisioner());
    }

    public JdbcJudgeRuntime(RuntimeDatabaseCluster databaseCluster, JudgeDefinitionStore definitionStore,
                            JudgeDialectProvider dialectProvider, RuntimeEnvironmentNamingStrategy namingStrategy,
                            SqlStatementParser statementParser, SqlDefinitionPolicy definitionPolicy,
                            RuntimeEnvironmentProvisioner environmentProvisioner) {
        this(databaseCluster, definitionStore, dialectProvider, namingStrategy,
                statementParser, definitionPolicy, environmentProvisioner,
                new NoOpJudgeTemplateStore(), new NoOpDatasetTemplateProvisioner());
    }

    public JdbcJudgeRuntime(RuntimeDatabaseCluster databaseCluster, JudgeDefinitionStore definitionStore,
                            JudgeDialectProvider dialectProvider, RuntimeEnvironmentNamingStrategy namingStrategy,
                            SqlStatementParser statementParser, SqlDefinitionPolicy definitionPolicy,
                            RuntimeEnvironmentProvisioner environmentProvisioner,
                            JudgeTemplateStore templateStore, DatasetTemplateProvisioner templateProvisioner) {
        // judge 런타임 구성 의존성 검증과 보관
        this.databaseCluster = Objects.requireNonNull(databaseCluster, "런타임 DB 클러스터가 필요합니다.");
        this.definitionStore = Objects.requireNonNull(definitionStore, "judge 정의 저장소가 필요합니다.");
        this.dialectProvider = Objects.requireNonNull(dialectProvider, "judge SQL 방언 제공자가 필요합니다.");
        this.namingStrategy = Objects.requireNonNull(namingStrategy, "실행 환경 이름 전략이 필요합니다.");
        this.statementParser = Objects.requireNonNull(statementParser, "SQL 문장 파서가 필요합니다.");
        this.definitionPolicy = Objects.requireNonNull(definitionPolicy, "SQL 정의 정책이 필요합니다.");
        this.executionPolicy = new SqlExecutionPolicy(this.statementParser);
        this.environmentProvisioner = Objects.requireNonNull(environmentProvisioner, "실행 환경 준비기가 필요합니다.");
        this.templateStore = Objects.requireNonNull(templateStore, "데이터셋 템플릿 저장소가 필요합니다.");
        this.templateProvisioner = Objects.requireNonNull(templateProvisioner, "데이터셋 템플릿 준비기가 필요합니다.");
        this.statisticsInitializer = new RuntimeStatisticsInitializer();
    }

    @Override
    public JudgeDatasetId createDataset(CreateJudgeDatasetInput command) {
        // 데이터셋 등록 입력 검증
        Objects.requireNonNull(command, "데이터셋 생성 입력이 필요합니다.");
        definitionPolicy.validateDdl(command.getDdl());
        definitionPolicy.validateDataSql(command.getDataSql());

        // 데이터셋 정의 저장과 템플릿 준비
        JudgeDatasetId datasetId = new JudgeDatasetId("dataset-" + UUID.randomUUID());
        DatasetDefinition datasetDefinition = new DatasetDefinition(
                datasetId, command.getDbmsType(), command.getDdl(),
                command.getDataSql(), command.getBaseIndexDdls()
        );
        definitionStore.saveDataset(datasetDefinition);
        templateProvisioner.prepare(datasetDefinition).ifPresent(templateStore::saveDatasetTemplate);

        return datasetId;
    }

    @Override
    public JudgeSetupSqlId createSetupSql(CreateJudgeSetupSqlInput command) {
        // 설정 SQL 등록 입력과 대상 데이터셋 검증
        Objects.requireNonNull(command, "설정 SQL 생성 입력이 필요합니다.");
        requireDataset(command.getDatasetId());
        definitionPolicy.validateSetupSqls(command.getSetupSqls());

        // 설정 SQL 정의 저장
        JudgeSetupSqlId setupSqlId = new JudgeSetupSqlId("setup-" + UUID.randomUUID());
        definitionStore.saveSetupSql(new SetupSqlDefinition(
                setupSqlId, command.getDatasetId(),
                command.getSetupSqls(), command.getIndexPolicy()
        ));

        return setupSqlId;
    }

    @Override
    public SqlReferenceResult createReference(CreateJudgeReferenceInput command) {
        // 기준 SQL 등록 입력 검증
        Objects.requireNonNull(command, "기준 SQL 생성 입력이 필요합니다.");
        definitionPolicy.validateReadOnlySql(command.getReferenceSql());

        // 기준 SQL 격리 실행과 결과 해시 생성
        JudgeExecutionId executionId = new JudgeExecutionId("reference-" + UUID.randomUUID());
        SqlExecutionResult executionResult = executeIsolated(new ExecuteIsolatedJudgeSqlInput(
                executionId, command.getDatasetId(), List.of(), command.getReferenceSql(),
                com.quertimizer.judge.domain.model.IsolationPolicy.cleanRoom(),
                command.getOptions()
        ));
        String resultHash = SqlResultHashSupport.hashResult(executionResult.getColumns(), executionResult.getRows());

        // 기준 SQL 정의 저장
        JudgeReferenceId referenceId = new JudgeReferenceId("reference-" + UUID.randomUUID());
        definitionStore.saveReference(new ReferenceDefinition(
                referenceId, command.getDatasetId(), command.getReferenceSql(), resultHash
        ));

        return new SqlReferenceResult(referenceId, resultHash, executionResult);
    }

    @Override
    public JudgeEnvironmentId create(CreateJudgeEnvironmentInput command) {
        // 실행 환경 생성 입력과 데이터셋 정의 조회
        Objects.requireNonNull(command, "실행 환경 생성 입력이 필요합니다.");
        DatasetDefinition dataset = requireDataset(command.getDatasetId());

        // 영속 실행 환경 준비와 메모리 등록
        JudgeEnvironmentId environmentId = new JudgeEnvironmentId("environment-" + UUID.randomUUID());
        log.info(
                "judge 실행 환경 생성 시작 environmentId={}, datasetId={}, dbmsType={}, reusable={}, baseIndexes={}, initializeStatistics={}",
                environmentId, dataset.getDatasetId(), dataset.getDbmsType(), command.getPolicy().isReusable(),
                command.getPolicy().isApplyBaseIndexes(), command.getPolicy().isInitializeStatisticsAfterLoad()
        );
        try {
            ProvisionedRuntimeEnvironment environment = environmentProvisioner.create(environmentId, dataset, command.getPolicy());
            environments.put(environmentId, new PersistentRuntimeEnvironment(environment));
            log.info(
                    "judge 실행 환경 생성 완료 environmentId={}, datasetId={}, strategy={}",
                    environmentId, dataset.getDatasetId(), environment.getProvisionerName()
            );
            return environmentId;
        } catch (RuntimeException exception) {
            log.warn(
                    "judge 실행 환경 생성 실패 environmentId={}, datasetId={}, dbmsType={}",
                    environmentId, dataset.getDatasetId(), dataset.getDbmsType(), exception
            );
            throw exception;
        }
    }

    @Override
    public JudgeExecutionId executeAsync(ExecuteJudgeSqlInput command, JudgeListener listener) {
        // 비동기 실행 입력과 리스너 검증
        Objects.requireNonNull(command, "SQL 실행 입력이 필요합니다.");
        Objects.requireNonNull(listener, "judge 실행 리스너가 필요합니다.");

        // 실행 접수 이벤트 발행과 비동기 실행 시작
        listener.onEvent(new ExecutionAccepted(command.getExecutionId()));
        CompletableFuture.runAsync(() -> emitExecutionResult(command, listener));

        return command.getExecutionId();
    }

    @Override
    public CompletionStage<SqlExecutionResult> executeAsync(ExecuteJudgeSqlInput command) {
        // 비동기 실행 입력 검증
        Objects.requireNonNull(command, "SQL 실행 입력이 필요합니다.");

        return CompletableFuture.supplyAsync(() -> execute(command));
    }

    @Override
    public SqlExecutionResult execute(ExecuteJudgeSqlInput command) {
        // 실행 입력 검증과 실행 대상 준비
        Objects.requireNonNull(command, "SQL 실행 입력이 필요합니다.");
        ExecutionMode mode = executionPolicy.resolveMode(command.getSql());
        String sql = resolveSingleStatement(command.getSql());
        PersistentRuntimeEnvironment persistentEnvironment = requireEnvironment(command.getEnvironmentId());
        log.info(
                "judge SQL 실행 시작 executionId={}, environmentId={}, mode={}, sqlLength={}, timeoutSeconds={}",
                command.getExecutionId(), command.getEnvironmentId(), mode, sql.length(),
                command.getOptions().getTimeoutSeconds()
        );

        // 영속 실행 환경 단위 동기화와 SQL 실행
        synchronized (persistentEnvironment.monitor) {
            persistentEnvironment.requireAvailable();
            try (RuntimeEnvironmentConnection environmentConnection = environmentProvisioner.openConnection(
                    persistentEnvironment.environment, command.getOptions().getTimeoutSeconds())) {
                Connection connection = environmentConnection.getConnection();
                try {
                    SqlExecutionResult result = executeInPersistentEnvironment(
                            command, sql, mode, connection,
                            environmentConnection.getDialect()
                    );
                    connection.commit();
                    log.info(
                            "judge SQL 실행 완료 executionId={}, environmentId={}, mode={}, rowCount={}, cost={}, executionTimeMs={}",
                            command.getExecutionId(), command.getEnvironmentId(), result.getMode(),
                            result.getRowCount(), result.getCost(), result.getExecutionTimeMs()
                    );
                    return result;
                } catch (Exception exception) {
                    rollback(connection);
                    throw exception;
                }
            } catch (Exception exception) {
                log.warn(
                        "judge SQL 실행 실패 executionId={}, environmentId={}, mode={}",
                        command.getExecutionId(), command.getEnvironmentId(), mode, exception
                );
                throw new IllegalStateException("judge 영속 실행 환경 SQL 실행 실패", exception);
            }
        }
    }

    @Override
    public JudgeExecutionId executeIsolatedAsync(ExecuteIsolatedJudgeSqlInput command, JudgeListener listener) {
        // 격리 비동기 실행 입력과 리스너 검증
        Objects.requireNonNull(command, "격리 SQL 실행 입력이 필요합니다.");
        Objects.requireNonNull(listener, "judge 실행 리스너가 필요합니다.");

        // 실행 접수 이벤트 발행과 비동기 실행 시작
        listener.onEvent(new ExecutionAccepted(command.getExecutionId()));
        CompletableFuture.runAsync(() -> emitIsolatedExecutionResult(command, listener));

        return command.getExecutionId();
    }

    @Override
    public CompletionStage<SqlExecutionResult> executeIsolatedAsync(ExecuteIsolatedJudgeSqlInput command) {
        // 격리 비동기 실행 입력 검증
        Objects.requireNonNull(command, "격리 SQL 실행 입력이 필요합니다.");

        return CompletableFuture.supplyAsync(() -> executeIsolated(command));
    }

    @Override
    public SqlExecutionResult executeIsolated(ExecuteIsolatedJudgeSqlInput command) {
        // 격리 실행 입력과 읽기 전용 SQL 검증
        Objects.requireNonNull(command, "격리 SQL 실행 입력이 필요합니다.");
        definitionPolicy.validateReadOnlySql(command.getTargetSql());

        // 격리 실행용 데이터셋과 설정 SQL 정의 조회
        DatasetDefinition dataset = requireDataset(command.getDatasetId());
        List<SetupSqlDefinition> setupSqlDefinitions = command.getSetupSqlIds().stream()
                .map(setupSqlId -> requireSetupSql(command.getDatasetId(), setupSqlId))
                .toList();

        return executeInTemporaryEnvironment(command, dataset, setupSqlDefinitions);
    }

    @Override
    public SqlExecutionResult analyze(AnalyzeJudgeEnvironmentInput command) {
        // 통계 갱신 입력과 실행 환경 조회
        Objects.requireNonNull(command, "통계 갱신 입력이 필요합니다.");
        PersistentRuntimeEnvironment persistentEnvironment = requireEnvironment(command.getEnvironmentId());
        log.info(
                "judge 통계 갱신 시작 executionId={}, environmentId={}, timeoutSeconds={}",
                command.getExecutionId(), command.getEnvironmentId(), command.getOptions().getTimeoutSeconds()
        );

        // 실행 환경 단위 동기화와 DBMS 통계 갱신
        synchronized (persistentEnvironment.monitor) {
            persistentEnvironment.requireAvailable();
            try (RuntimeEnvironmentConnection environmentConnection = environmentProvisioner.openConnection(
                    persistentEnvironment.environment, command.getOptions().getTimeoutSeconds())) {
                Connection connection = environmentConnection.getConnection();
                try {
                    SqlExecutionResult result = executeAnalyze(command.getExecutionId(), connection,
                            environmentConnection.getDialect(), environmentConnection.getEnvironmentName());
                    connection.commit();
                    log.info(
                            "judge 통계 갱신 완료 executionId={}, environmentId={}, executionTimeMs={}",
                            command.getExecutionId(), command.getEnvironmentId(), result.getExecutionTimeMs()
                    );
                    return result;
                } catch (Exception exception) {
                    rollback(connection);
                    throw exception;
                }
            } catch (Exception exception) {
                log.warn(
                        "judge 통계 갱신 실패 executionId={}, environmentId={}",
                        command.getExecutionId(), command.getEnvironmentId(), exception
                );
                throw new IllegalStateException("judge 런타임 통계 갱신 실패", exception);
            }
        }
    }

    @Override
    public void cancel(JudgeExecutionId executionId) {
        // 취소 대상 실행 ID와 추적 문장 확인
        Objects.requireNonNull(executionId, "취소 대상 실행 ID가 필요합니다.");

        Statement activeStatement = activeStatements.remove(executionId);
        if (activeStatement == null) {
            return;
        }

        // 실행 중 문장 취소와 리소스 정리
        try {
            activeStatement.cancel();
        } catch (Exception ignored) {
        }

        try {
            activeStatement.close();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void drop(JudgeEnvironmentId environmentId) {
        // 제거 대상 실행 환경 조회
        Objects.requireNonNull(environmentId, "제거 대상 실행 환경 ID가 필요합니다.");

        PersistentRuntimeEnvironment persistentEnvironment = environments.get(environmentId);
        if (persistentEnvironment == null) {
            log.info("judge 실행 환경 제거 대상 없음 environmentId={}", environmentId);
            return;
        }

        // 실행 환경 단위 동기화와 영속 실행 환경 제거
        synchronized (persistentEnvironment.monitor) {
            if (persistentEnvironment.dropped) {
                log.info("judge 실행 환경 제거 중복 요청 무시 environmentId={}", environmentId);
                return;
            }

            persistentEnvironment.dropped = true;
            try {
                log.info("judge 실행 환경 제거 시작 environmentId={}", environmentId);
                environmentProvisioner.drop(persistentEnvironment.environment);
                environments.remove(environmentId, persistentEnvironment);
                log.info("judge 실행 환경 제거 완료 environmentId={}", environmentId);
            } catch (Exception exception) {
                persistentEnvironment.dropped = false;
                log.warn("judge 실행 환경 제거 실패 environmentId={}", environmentId, exception);
                throw new IllegalStateException("judge 영속 실행 환경 제거 실패", exception);
            }
        }
    }

    private RuntimeEnvironment createRuntimeEnvironment(JudgeEnvironmentId environmentId, DatasetDefinition dataset,
                                                        RuntimeDatabase database) {
        // 런타임 실행 환경 메타데이터 생성
        return new RuntimeEnvironment(
                environmentId, dataset.getDatasetId(),
                database,
                namingStrategy.createName(environmentId, dataset.getDatasetId()),
                Instant.now()
        );
    }

    private SqlExecutionResult executeInTemporaryEnvironment(ExecuteIsolatedJudgeSqlInput command, DatasetDefinition dataset,
                                                            List<SetupSqlDefinition> setupSqlDefinitions) {
        // 격리 실행용 런타임 DB 점유와 커넥션 생성
        try (RuntimeDatabaseLease lease = databaseCluster.acquire(dataset.getDbmsType());
             Connection connection = lease.openConnection()) {
            JudgeEnvironmentId environmentId = new JudgeEnvironmentId("environment-" + UUID.randomUUID());
            RuntimeEnvironment environment = createRuntimeEnvironment(environmentId, dataset, lease.getDatabase());
            JudgeDialect dialect = dialectProvider.get(dataset.getDbmsType());

            return executeWithTemporaryEnvironment(command, dataset, setupSqlDefinitions, connection, dialect, environment);
        } catch (Exception exception) {
            throw new IllegalStateException("judge 격리 실행 실패", exception);
        }
    }

    private SqlExecutionResult executeWithTemporaryEnvironment(ExecuteIsolatedJudgeSqlInput command, DatasetDefinition dataset,
                                                              List<SetupSqlDefinition> setupSqlDefinitions, Connection connection,
                                                              JudgeDialect dialect,
                                                              RuntimeEnvironment environment) throws Exception {
        // 격리 실행 환경 생성과 데이터셋 적재
        String environmentName = environment.getName().getValue();
        connection.setAutoCommit(false);
        try {
            createEnvironment(connection, dialect, environmentName);
            loadDataset(connection, dialect, environmentName, dataset, true, command.getIsolationPolicy().isInitializeStatisticsAfterLoad());
            if (command.getIsolationPolicy().isApplySetupSqls()) {
                executeSetupSqls(connection, setupSqlDefinitions);
            }
            if (command.getIsolationPolicy().isInitializeStatisticsAfterSetup()) {
                initializeStatistics(connection, dialect, environmentName);
            }

            // 실행 환경 설정과 대상 SQL 실행
            configureExecutionConnection(connection, dialect, environmentName, command.getOptions().getTimeoutSeconds());
            SqlExecutionResult result = executeSelectAll(
                    command.getExecutionId(),
                    connection,
                    dialect,
                    command.getTargetSql(),
                    command.getOptions().isIncludeCost(),
                    command.getOptions().isIncludePlan()
            );
            connection.commit();
            return result;
        } catch (Exception exception) {
            rollback(connection);
            throw exception;
        } finally {
            // 격리 실행 환경 정리 정책 적용
            if (command.getIsolationPolicy().isDropEnvironmentAfterExecution()) {
                cleanupEnvironment(connection, dialect, environmentName);
            }
        }
    }

    private SqlExecutionResult executeInPersistentEnvironment(ExecuteJudgeSqlInput command, String sql,
                                                             ExecutionMode mode, Connection connection,
                                                             JudgeDialect dialect) throws Exception {
        // SQL 실행 모드별 처리 흐름 분기
        return switch (mode) {
            case SELECT -> executeSelectPage(
                    command.getExecutionId(),
                    connection,
                    dialect,
                    sql,
                    command.getOptions().getPage(),
                    command.getOptions().getPageSize(),
                    command.getOptions().isIncludeCost(),
                    command.getOptions().isIncludePlan()
            );
            case EXPLAIN, EXPLAIN_ANALYZE -> executePlan(command.getExecutionId(), connection, sql, mode);
            case ANALYZE -> throw new IllegalArgumentException("ANALYZE는 analyze API로 실행해야 합니다.");
            case INDEX_COMMAND -> executeCommand(command.getExecutionId(), connection, sql, mode);
            case COMMAND -> executeCommand(command.getExecutionId(), connection, sql, mode);
        };
    }

    private void createEnvironment(Connection connection, JudgeDialect dialect, String environmentName) throws Exception {
        // 기존 실행 환경 제거 후 새 실행 환경 생성
        try (Statement statement = connection.createStatement()) {
            statement.execute(dialect.dropEnvironmentIfExistsSql(environmentName));
            statement.execute(dialect.createEnvironmentSql(environmentName));
            for (String useEnvironmentSql : dialect.useEnvironmentSqls(environmentName)) {
                statement.execute(useEnvironmentSql);
            }
        }
    }

    private void loadDataset(Connection connection, JudgeDialect dialect,
                             String environmentName, DatasetDefinition dataset,
                             boolean applyBaseIndexes,
                             boolean initializeStatistics) throws Exception {
        // 데이터셋 DDL과 데이터 SQL 실행
        executeStatements(connection, dataset.getDdl());
        executeStatements(connection, dataset.getDataSql());

        // 기준 인덱스와 통계 초기화 정책 적용
        if (applyBaseIndexes) {
            for (String baseIndexDdl : dataset.getBaseIndexDdls()) {
                executeStatements(connection, baseIndexDdl);
            }
        }
        if (initializeStatistics) {
            initializeStatistics(connection, dialect, environmentName);
        }
    }

    private void executeSetupSqls(Connection connection, List<SetupSqlDefinition> setupSqlDefinitions) throws Exception {
        // 등록된 설정 SQL 순차 실행
        for (SetupSqlDefinition setupSqlDefinition : setupSqlDefinitions) {
            for (String setupSql : setupSqlDefinition.getSetupSqls()) {
                executeStatements(connection, setupSql);
            }
        }
    }

    private void initializeStatistics(Connection connection, JudgeDialect dialect, String environmentName) throws Exception {
        // 현재 실행 환경 DBMS 통계 초기화
        statisticsInitializer.initialize(connection, dialect, environmentName);
    }

    private void initializeStatistics(JudgeExecutionId executionId, Connection connection,
                                      JudgeDialect dialect, String environmentName) throws Exception {
        // 취소 추적 가능한 DBMS 통계 초기화
        statisticsInitializer.initialize(executionId, activeStatements, connection, dialect, environmentName);
    }

    private void executeStatements(Connection connection, String sql) throws Exception {
        // SQL 문자열을 실행 가능한 문장으로 분리해 순차 실행
        for (String statementSql : statementParser.splitStatements(sql)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(statementSql);
            }
        }
    }

    private void configureExecutionConnection(Connection connection, JudgeDialect dialect,
                                              String environmentName,
                                              int timeoutSeconds) throws Exception {
        // 실행 환경 선택과 문장 제한 시간 설정
        try (Statement statement = connection.createStatement()) {
            for (String useEnvironmentSql : dialect.useEnvironmentSqls(environmentName)) {
                statement.execute(useEnvironmentSql);
            }
            for (String timeoutSql : dialect.statementTimeoutSqls(timeoutSeconds)) {
                statement.execute(timeoutSql);
            }
        }
    }

    private SqlExecutionResult executeSelectPage(JudgeExecutionId executionId, Connection connection,
                                                 JudgeDialect dialect, String sql,
                                                 int page, int pageSize,
                                                 boolean includeCost,
                                                 boolean includePlan) throws Exception {
        // 실행 계획과 비용, 전체 행 수, 현재 페이지 결과 조회
        long startTime = System.nanoTime();
        List<String> planLines = includeCost || includePlan ? executePlanLines(executionId, connection, dialect.explainSql(sql)) : List.of();
        BigDecimal cost = includeCost ? SqlPlanCostParser.extractEstimatedCost(planLines) : null;
        long rowCount = fetchSelectRowCount(executionId, connection, dialect, sql);
        int totalPages = Math.max(1, (int) Math.ceil((double) rowCount / pageSize));
        int currentPage = Math.min(page, totalPages);
        SqlTableResult pageResult = fetchSelectPage(executionId, connection, dialect, sql, currentPage, pageSize);

        return new SqlExecutionResult(
                ExecutionMode.SELECT,
                pageResult.columns,
                pageResult.rows,
                rowCount,
                currentPage,
                pageSize,
                Duration.ofNanos(System.nanoTime() - startTime).toMillis(),
                cost,
                includePlan ? planLines : List.of(),
                "SQL 실행 완료"
        );
    }

    private SqlExecutionResult executeSelectAll(JudgeExecutionId executionId, Connection connection,
                                                JudgeDialect dialect, String sql,
                                                boolean includeCost,
                                                boolean includePlan) throws Exception {
        // 단일 SELECT 문장 정리와 전체 결과 조회
        String statementSql = resolveSingleStatement(sql);
        long startTime = System.nanoTime();
        List<String> planLines = includeCost || includePlan ? executePlanLines(executionId, connection, dialect.explainSql(statementSql)) : List.of();
        BigDecimal cost = includeCost ? SqlPlanCostParser.extractEstimatedCost(planLines) : null;
        SqlTableResult tableResult = fetchSelectAll(executionId, connection, statementSql);

        return new SqlExecutionResult(
                ExecutionMode.SELECT,
                tableResult.columns,
                tableResult.rows,
                tableResult.rows.size(),
                1,
                Math.max(tableResult.rows.size(), 1),
                Duration.ofNanos(System.nanoTime() - startTime).toMillis(),
                cost,
                includePlan ? planLines : List.of(),
                "SQL 실행 완료"
        );
    }

    private SqlExecutionResult executePlan(JudgeExecutionId executionId, Connection connection,
                                           String sql,
                                           ExecutionMode mode) throws Exception {
        // 실행 계획 SQL 실행과 비용 추출
        long startTime = System.nanoTime();
        List<String> planLines = executePlanLines(executionId, connection, sql);

        return new SqlExecutionResult(
                mode,
                List.of(),
                List.of(),
                planLines.size(),
                1,
                Math.max(planLines.size(), 1),
                Duration.ofNanos(System.nanoTime() - startTime).toMillis(),
                SqlPlanCostParser.extractEstimatedCost(planLines),
                planLines,
                "SQL 실행 계획 반환"
        );
    }

    private SqlExecutionResult executeAnalyze(JudgeExecutionId executionId, Connection connection,
                                              JudgeDialect dialect, String environmentName) throws Exception {
        // DBMS 통계 갱신 실행과 결과 생성
        long startTime = System.nanoTime();
        initializeStatistics(executionId, connection, dialect, environmentName);

        return new SqlExecutionResult(
                ExecutionMode.ANALYZE,
                List.of(),
                List.of(),
                0,
                1,
                1,
                Duration.ofNanos(System.nanoTime() - startTime).toMillis(),
                null,
                List.of(),
                "SQL 통계 갱신 완료"
        );
    }

    private SqlExecutionResult executeCommand(JudgeExecutionId executionId, Connection connection,
                                              String sql,
                                              ExecutionMode mode) throws Exception {
        // DDL 또는 명령 SQL 실행과 변경 행 수 반환
        long startTime = System.nanoTime();
        Statement statement = createTrackedStatement(executionId, connection);
        try (statement) {
            statement.execute(sql);
            int updateCount = Math.max(statement.getUpdateCount(), 0);

            return new SqlExecutionResult(
                    mode,
                    List.of(),
                    List.of(),
                    updateCount,
                    1,
                    1,
                    Duration.ofNanos(System.nanoTime() - startTime).toMillis(),
                    null,
                    List.of(),
                    "SQL 명령 실행 완료"
            );
        } finally {
            clearTrackedStatement(executionId, statement);
        }
    }

    private long fetchSelectRowCount(JudgeExecutionId executionId, Connection connection,
                                     JudgeDialect dialect,
                                     String sql) throws Exception {
        // SELECT 전체 행 수 조회
        Statement statement = createTrackedStatement(executionId, connection);
        try (statement) {
            statement.execute(dialect.selectCountSql(sql));

            try (ResultSet resultSet = statement.getResultSet()) {
                if (resultSet == null || !resultSet.next()) {
                    return 0;
                }

                return resultSet.getLong(1);
            }
        } finally {
            clearTrackedStatement(executionId, statement);
        }
    }

    private SqlTableResult fetchSelectPage(JudgeExecutionId executionId, Connection connection,
                                           JudgeDialect dialect, String sql,
                                           int page,
                                           int pageSize) throws Exception {
        // SELECT 페이지 결과 조회
        PreparedStatement statement = createTrackedPreparedStatement(executionId, connection, dialect.selectPageSql(sql));
        try (statement) {
            statement.setInt(1, pageSize);
            statement.setLong(2, (long) (page - 1) * pageSize);
            statement.execute();

            try (ResultSet resultSet = statement.getResultSet()) {
                if (resultSet == null) {
                    throw new IllegalArgumentException("SQL이 결과 집합을 반환하지 않았습니다.");
                }

                return readTableResult(resultSet);
            }
        } finally {
            clearTrackedStatement(executionId, statement);
        }
    }

    private SqlTableResult fetchSelectAll(JudgeExecutionId executionId, Connection connection, String sql) throws Exception {
        // SELECT 전체 결과 조회
        Statement statement = createTrackedStatement(executionId, connection);
        try (statement) {
            statement.execute(sql);

            try (ResultSet resultSet = statement.getResultSet()) {
                if (resultSet == null) {
                    throw new IllegalArgumentException("SQL이 결과 집합을 반환하지 않았습니다.");
                }

                return readTableResult(resultSet);
            }
        } finally {
            clearTrackedStatement(executionId, statement);
        }
    }

    private List<String> executePlanLines(JudgeExecutionId executionId, Connection connection, String sql) throws Exception {
        // 실행 계획 결과 라인 조회
        Statement statement = createTrackedStatement(executionId, connection);
        try (statement) {
            statement.execute(sql);

            try (ResultSet resultSet = statement.getResultSet()) {
                if (resultSet == null) {
                    throw new IllegalArgumentException("SQL이 실행 계획을 반환하지 않았습니다.");
                }

                return readPlanLines(resultSet);
            }
        } finally {
            clearTrackedStatement(executionId, statement);
        }
    }

    private SqlTableResult readTableResult(ResultSet resultSet) throws Exception {
        // 결과 집합 메타데이터 기준 컬럼명 추출
        ResultSetMetaData metaData = resultSet.getMetaData();
        List<String> columns = new ArrayList<>();
        for (int columnIndex = 1; columnIndex <= metaData.getColumnCount(); columnIndex++) {
            columns.add(metaData.getColumnLabel(columnIndex));
        }

        // 결과 집합 행 데이터 문자열 변환
        List<List<String>> rows = new ArrayList<>();
        while (resultSet.next()) {
            List<String> row = new ArrayList<>();
            for (int columnIndex = 1; columnIndex <= metaData.getColumnCount(); columnIndex++) {
                Object value = resultSet.getObject(columnIndex);
                row.add(value != null ? String.valueOf(value) : "null");
            }
            rows.add(row);
        }

        return new SqlTableResult(columns, rows);
    }

    private List<String> readPlanLines(ResultSet resultSet) throws Exception {
        // 실행 계획 결과 집합을 문자열 라인으로 변환
        ResultSetMetaData metaData = resultSet.getMetaData();
        List<String> planLines = new ArrayList<>();
        while (resultSet.next()) {
            if (metaData.getColumnCount() == 1) {
                planLines.add(String.valueOf(resultSet.getObject(1)));
                continue;
            }

            List<String> values = new ArrayList<>();
            for (int columnIndex = 1; columnIndex <= metaData.getColumnCount(); columnIndex++) {
                Object value = resultSet.getObject(columnIndex);
                values.add(metaData.getColumnLabel(columnIndex) + "=" + (value != null ? value : ""));
            }
            planLines.add(String.join(", ", values));
        }

        return planLines;
    }

    private Statement createTrackedStatement(JudgeExecutionId executionId, Connection connection) throws Exception {
        // 실행 취소용 Statement 추적 등록
        Statement statement = connection.createStatement();
        activeStatements.put(executionId, statement);
        return statement;
    }

    private PreparedStatement createTrackedPreparedStatement(JudgeExecutionId executionId, Connection connection,
                                                             String sql) throws Exception {
        // 실행 취소용 PreparedStatement 추적 등록
        PreparedStatement statement = connection.prepareStatement(sql);
        activeStatements.put(executionId, statement);
        return statement;
    }

    private void clearTrackedStatement(JudgeExecutionId executionId, Statement statement) {
        // 실행 취소 추적 Statement 제거
        activeStatements.remove(executionId, statement);
    }

    private void cleanupEnvironment(Connection connection, JudgeDialect dialect, String environmentName) {
        // 격리 실행 환경 정리 실패 무시
        try {
            dropEnvironment(connection, dialect, environmentName);
        } catch (Exception ignored) {
        }
    }

    private void dropEnvironment(Connection connection, JudgeDialect dialect, String environmentName) throws Exception {
        // 실행 환경 제거 SQL 실행
        try (Statement statement = connection.createStatement()) {
            connection.setAutoCommit(true);
            statement.execute(dialect.dropEnvironmentIfExistsSql(environmentName));
        }
    }

    private void rollback(Connection connection) {
        // 실패 트랜잭션 롤백
        try {
            connection.rollback();
        } catch (Exception ignored) {
        }
    }

    private String resolveSingleStatement(String sql) {
        // 단일 SQL 문장 분리
        List<String> statements = statementParser.splitStatements(sql);
        if (statements.size() != 1) {
            throw new IllegalArgumentException("SQL 문장은 하나만 허용됩니다.");
        }

        return statements.get(0);
    }

    private DatasetDefinition requireDataset(JudgeDatasetId datasetId) {
        // 데이터셋 정의 조회
        return definitionStore.findDataset(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 데이터셋 ID: " + datasetId));
    }

    private SetupSqlDefinition requireSetupSql(JudgeDatasetId datasetId, JudgeSetupSqlId setupSqlId) {
        // 설정 SQL 정의 조회
        SetupSqlDefinition setupSqlDefinition = definitionStore.findSetupSql(setupSqlId)
                .orElseThrow(() -> new IllegalArgumentException("알 수 없는 설정 SQL 묶음 ID: " + setupSqlId));

        // 설정 SQL 대상 데이터셋 일치 여부 검증
        if (!setupSqlDefinition.getDatasetId().equals(datasetId)) {
            throw new IllegalArgumentException("설정 SQL 묶음의 대상 데이터셋이 다릅니다: " + setupSqlId);
        }

        return setupSqlDefinition;
    }

    private PersistentRuntimeEnvironment requireEnvironment(JudgeEnvironmentId environmentId) {
        // 영속 실행 환경 조회
        PersistentRuntimeEnvironment environment = environments.get(environmentId);
        if (environment == null) {
            throw new IllegalArgumentException("알 수 없는 실행 환경 ID: " + environmentId);
        }

        return environment;
    }

    private void emitExecutionResult(ExecuteJudgeSqlInput command, JudgeListener listener) {
        // 영속 실행 결과 이벤트 발행
        try {
            SqlExecutionResult result = execute(command);
            listener.onEvent(new ExecutionCompleted(command.getExecutionId(), result));
        } catch (Exception exception) {
            listener.onEvent(new ExecutionFailed(command.getExecutionId(), exception.getMessage(), exception));
        }
    }

    private void emitIsolatedExecutionResult(ExecuteIsolatedJudgeSqlInput command, JudgeListener listener) {
        // 격리 실행 결과 이벤트 발행
        try {
            SqlExecutionResult result = executeIsolated(command);
            listener.onEvent(new ExecutionCompleted(command.getExecutionId(), result));
        } catch (Exception exception) {
            listener.onEvent(new ExecutionFailed(command.getExecutionId(), exception.getMessage(), exception));
        }
    }

    private static final class PersistentRuntimeEnvironment {
        private final ProvisionedRuntimeEnvironment environment;
        private final Object monitor = new Object();
        private boolean dropped;

        private PersistentRuntimeEnvironment(ProvisionedRuntimeEnvironment environment) {
            this.environment = Objects.requireNonNull(environment, "준비된 실행 환경이 필요합니다.");
        }

        private void requireAvailable() {
            // 제거된 실행 환경 사용 차단
            if (dropped) {
                throw new IllegalStateException(
                        "이미 제거된 실행 환경입니다: "
                                + environment.getRuntimeEnvironment().getEnvironmentId()
                );
            }
        }
    }

    private static final class SqlTableResult {
        private final List<String> columns;
        private final List<List<String>> rows;

        private SqlTableResult(List<String> columns, List<List<String>> rows) {
            this.columns = List.copyOf(Objects.requireNonNull(columns, "컬럼 목록이 필요합니다."));
            this.rows = rows.stream()
                    .map(row -> List.copyOf(Objects.requireNonNull(row, "결과 행이 필요합니다.")))
                    .toList();
        }
    }
}
