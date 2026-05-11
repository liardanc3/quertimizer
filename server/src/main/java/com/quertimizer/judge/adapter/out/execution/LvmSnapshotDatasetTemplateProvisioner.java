package com.quertimizer.judge.adapter.out.execution;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.application.service.JudgeDialect;
import com.quertimizer.judge.application.service.JudgeDialectService;
import com.quertimizer.judge.application.service.SqlStatementParser;
import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.DatasetTemplateDefinition;
import com.quertimizer.judge.domain.model.JudgeQueuePriority;
import com.quertimizer.judge.domain.model.JudgeQueueStatusListener;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
public class LvmSnapshotDatasetTemplateProvisioner implements DatasetTemplateProvisioner {

    private final JudgeDialectService dialectService;
    private final LvmSnapshotRuntimeOptions options;
    private final LvmSnapshotCommandExecutor commandExecutor;
    private final LvmSnapshotRuntimeCommandFactory commandFactory;
    private final SqlStatementParser statementParser;
    private final RuntimeStatisticsInitializer statisticsInitializer;
    private final LvmSnapshotRuntimeResourceManager resourceManager;

    public LvmSnapshotDatasetTemplateProvisioner(RuntimeDatabaseCluster databaseCluster,
                                                 JudgeDialectService dialectService,
                                                 LvmSnapshotRuntimeOptions options,
                                                 LvmSnapshotCommandExecutor commandExecutor,
                                                 LvmSnapshotRuntimeCommandFactory commandFactory,
                                                 SqlStatementParser statementParser) {
        this(dialectService, options, commandExecutor, commandFactory, statementParser,
                new LvmSnapshotRuntimeResourceManager(databaseCluster, options));
    }

    public LvmSnapshotDatasetTemplateProvisioner(JudgeDialectService dialectService,
                                                 LvmSnapshotRuntimeOptions options,
                                                 LvmSnapshotCommandExecutor commandExecutor,
                                                 LvmSnapshotRuntimeCommandFactory commandFactory,
                                                 SqlStatementParser statementParser,
                                                 LvmSnapshotRuntimeResourceManager resourceManager) {
        this.dialectService = Objects.requireNonNull(dialectService, "필수 값이 없습니다.");
        this.options = Objects.requireNonNull(options, "필수 값이 없습니다.");
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "필수 값이 없습니다.");
        this.commandFactory = Objects.requireNonNull(commandFactory, "필수 값이 없습니다.");
        this.statementParser = Objects.requireNonNull(statementParser, "필수 값이 없습니다.");
        this.statisticsInitializer = new RuntimeStatisticsInitializer();
        this.resourceManager = Objects.requireNonNull(resourceManager, "필수 값이 없습니다.");
    }

    @Override
    public Optional<DatasetTemplateDefinition> prepare(DatasetDefinition dataset) {
        return prepare(dataset, JudgeQueuePriority.FIRST);
    }

    @Override
    public Optional<DatasetTemplateDefinition> prepare(DatasetDefinition dataset, JudgeQueuePriority queuePriority) {
        // 데이터셋 템플릿 준비 입력 검증과 시작 로그 기록
        Objects.requireNonNull(dataset, "필수 값이 없습니다.");
        log.info(
                "lvm-snapshot 데이터셋 템플릿 준비 시작 datasetId={}, dbmsType={}",
                dataset.getDatasetId().getValue(), dataset.getDbmsType()
        );

        // 템플릿 생성용 runner와 snapshot 이름 정보 확보
        LvmSnapshotRuntimeSlot runtimeSlot = resourceManager.acquire(
                dataset.getDbmsType(), queuePriority, JudgeQueueStatusListener.noop()
        );
        RuntimeDatabase runnerDatabase = runtimeSlot.getRunnerDatabase();
        LvmSnapshotRuntimeNode runtimeNode = options.requireNode(runnerDatabase.getId());
        String scriptDbmsName = LvmSnapshotNameSupport.scriptDbmsName(dataset.getDbmsType());
        String templateVersion = LvmSnapshotNameSupport.scriptName(dataset.getDatasetId().getValue());
        RuntimeEnvironmentName environmentName = LvmSnapshotNameSupport.datasetEnvironmentName(dataset.getDatasetId().getValue());
        int port = runtimeSlot.getPort();

        boolean templateCreationRequested = false;
        boolean processStarted = false;
        try {
            // 기준 템플릿 기반 유지보수 snapshot 생성
            templateCreationRequested = true;
            log.info(
                    "lvm-snapshot 데이터셋 템플릿 snapshot 생성 시작 datasetId={}, runnerId={}, templateVersion={}",
                    dataset.getDatasetId().getValue(), runnerDatabase.getId(), templateVersion
            );
            createMaintenanceTemplate(scriptDbmsName, templateVersion);
            log.info(
                    "lvm-snapshot 데이터셋 템플릿 snapshot 생성 완료 datasetId={}, templateVersion={}",
                    dataset.getDatasetId().getValue(), templateVersion
            );

            // 유지보수 snapshot을 바라보는 DB 프로세스 시작
            log.info(
                    "lvm-snapshot 데이터셋 템플릿 DB 프로세스 시작 datasetId={}, runnerContainer={}, port={}",
                    dataset.getDatasetId().getValue(), runtimeNode.getRunnerContainer(), port
            );
            startTemplateProcess(dataset.getDbmsType(), runtimeNode, templateVersion, port);
            processStarted = true;

            // 템플릿 DB 준비 대기 후 DDL과 데이터 적재
            RuntimeDatabase templateDatabase = createTemplateDatabase(runnerDatabase, runtimeNode, templateVersion, port);
            log.info(
                    "lvm-snapshot 데이터셋 템플릿 DB 준비 대기 시작 datasetId={}, jdbcUrl={}",
                    dataset.getDatasetId().getValue(), templateDatabase.getJdbcUrl()
            );
            waitUntilReady(templateDatabase);
            log.info("lvm-snapshot 데이터셋 적재 시작 datasetId={}, environmentName={}", dataset.getDatasetId().getValue(), environmentName.getValue());
            loadDataset(templateDatabase, environmentName.getValue(), dataset);
            log.info("lvm-snapshot 데이터셋 적재 완료 datasetId={}, environmentName={}", dataset.getDatasetId().getValue(), environmentName.getValue());
            stopTemplateProcess(dataset.getDbmsType(), runtimeNode, templateVersion, runnerDatabase.getPassword());
            processStarted = false;

            // 데이터 적재가 끝난 템플릿 snapshot 읽기 전용 봉인
            log.info("lvm-snapshot 데이터셋 템플릿 봉인 시작 datasetId={}, templateVersion={}", dataset.getDatasetId().getValue(), templateVersion);
            sealTemplate(scriptDbmsName, templateVersion);
            log.info("lvm-snapshot 데이터셋 템플릿 준비 완료 datasetId={}, templateVersion={}", dataset.getDatasetId().getValue(), templateVersion);

            // 준비된 데이터셋 템플릿 정의 반환
            return Optional.of(new DatasetTemplateDefinition(
                    dataset.getDatasetId(),
                    dataset.getDbmsType(),
                    templateVersion,
                    environmentName.getValue(),
                    Instant.now()
            ));
        } catch (Exception exception) {
            // 템플릿 준비 실패 시 프로세스와 실패 snapshot 정리
            log.warn(
                    "lvm-snapshot 데이터셋 템플릿 준비 실패 datasetId={}, dbmsType={}, templateVersion={}",
                    dataset.getDatasetId().getValue(), dataset.getDbmsType(), templateVersion, exception
            );
            try {
                cleanupFailedTemplate(
                        dataset.getDbmsType(), runtimeNode, scriptDbmsName, templateVersion,
                        runnerDatabase.getPassword(), processStarted, templateCreationRequested
                );
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw new IllegalStateException("LVM 스냅샷 데이터셋 템플릿 준비 실패", exception);
        } finally {
            // 템플릿 준비용 포트와 runner lease 반환
            resourceManager.release(runtimeSlot);
        }
    }

    @Override
    public void drop(DatasetTemplateDefinition templateDefinition) {
        // 데이터셋 템플릿 제거 입력 검증과 시작 로그 기록
        Objects.requireNonNull(templateDefinition, "필수 값이 없습니다.");
        String scriptDbmsName = LvmSnapshotNameSupport.scriptDbmsName(templateDefinition.getDbmsType());
        log.info(
                "lvm-snapshot 데이터셋 템플릿 제거 시작 datasetId={}, templateVersion={}",
                templateDefinition.getDatasetId().getValue(), templateDefinition.getTemplateVersion()
        );

        // 봉인된 템플릿 snapshot 제거
        dropTemplate(scriptDbmsName, templateDefinition.getTemplateVersion());
        log.info(
                "lvm-snapshot 데이터셋 템플릿 제거 완료 datasetId={}, templateVersion={}",
                templateDefinition.getDatasetId().getValue(), templateDefinition.getTemplateVersion()
        );
    }

    private void createMaintenanceTemplate(String scriptDbmsName, String templateVersion) {
        executeCommands(commandFactory.createMaintenanceTemplateCommands(
                scriptDbmsName, options.getBaseTemplateVersion(),
                templateVersion));
    }

    private void startTemplateProcess(DbmsType dbmsType, LvmSnapshotRuntimeNode runtimeNode,
                                      String templateVersion, int port) {
        executeCommands(commandFactory.startTemplateProcessCommands(
                dbmsType, runtimeNode.getRunnerContainer(),
                templateVersion, port));
    }

    private void stopTemplateProcess(DbmsType dbmsType, LvmSnapshotRuntimeNode runtimeNode,
                                     String templateVersion, String runnerPassword) {
        commandExecutor.execute(commandFactory.stopTemplateProcessCommand(
                dbmsType, runtimeNode.getRunnerContainer(),
                templateVersion, mysqlRootPassword(runtimeNode, runnerPassword)));
    }

    private void sealTemplate(String scriptDbmsName, String templateVersion) {
        executeCommands(commandFactory.sealTemplateCommands(scriptDbmsName, templateVersion));
    }

    private void dropTemplate(String scriptDbmsName, String templateVersion) {
        executeCommands(commandFactory.dropTemplateCommands(scriptDbmsName, templateVersion));
    }

    private RuntimeDatabase createTemplateDatabase(RuntimeDatabase runnerDatabase, LvmSnapshotRuntimeNode runtimeNode,
                                                   String templateVersion, int port) {
        return new RuntimeDatabase(
                runnerDatabase.getId() + "-template-" + templateVersion,
                runnerDatabase.getName() + "-template-" + templateVersion,
                runnerDatabase.getDbmsType(),
                createJdbcUrl(runnerDatabase.getDbmsType(), runtimeNode, port),
                runnerDatabase.getUsername(),
                runnerDatabase.getPassword(),
                true,
                1,
                runnerDatabase.getWeight()
        );
    }

    private String createJdbcUrl(DbmsType dbmsType, LvmSnapshotRuntimeNode runtimeNode, int port) {
        return switch (dbmsType) {
            case POSTGRESQL -> "jdbc:postgresql://" + runtimeNode.getHost() + ":" + port + "/" + runtimeNode.getDatabaseName();
            case MYSQL -> "jdbc:mysql://" + runtimeNode.getHost() + ":" + port + "/" + runtimeNode.getDatabaseName()
                    + "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=UTC";
        };
    }

    private void loadDataset(RuntimeDatabase templateDatabase, String environmentName, DatasetDefinition dataset) throws Exception {
        JudgeDialect dialect = dialectService.get(templateDatabase.getDbmsType());
        try (RuntimeDatabaseLease lease = new RuntimeDatabaseLease(templateDatabase);
             Connection connection = lease.openConnection()) {
            connection.setAutoCommit(false);
            try {
                createEnvironment(connection, dialect, environmentName);
                executeStatements(connection, dataset.getDdl());
                executeStatements(connection, dataset.getDataSql());
                for (String baseIndexDdl : dataset.getBaseIndexDdls()) {
                    executeStatements(connection, baseIndexDdl);
                }
                statisticsInitializer.initialize(connection, dialect, environmentName);
                connection.commit();
            } catch (Exception exception) {
                rollback(connection);
                throw exception;
            }
        }
    }

    private void waitUntilReady(RuntimeDatabase templateDatabase) {
        // 템플릿 DB 프로세스가 JDBC 연결을 받을 때까지 대기
        Exception lastFailure = null;
        Instant deadline = Instant.now().plusSeconds(options.getStartupTimeoutSeconds());
        while (!Instant.now().isAfter(deadline)) {
            try (RuntimeDatabaseLease lease = new RuntimeDatabaseLease(templateDatabase);
                 Connection connection = lease.openConnection();
                 Statement statement = connection.createStatement()) {
                statement.execute("SELECT 1");
                return;
            } catch (Exception exception) {
                lastFailure = exception;
                sleepBeforeRetry();
            }
        }

        throw new IllegalStateException("LVM 스냅샷 템플릿 DB 프로세스 준비 실패", lastFailure);
    }

    private void createEnvironment(Connection connection, JudgeDialect dialect, String environmentName) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute(dialect.dropEnvironmentIfExistsSql(environmentName));
            statement.execute(dialect.createEnvironmentSql(environmentName));
            for (String useEnvironmentSql : dialect.useEnvironmentSqls(environmentName)) {
                statement.execute(useEnvironmentSql);
            }
        }
    }

    private void executeStatements(Connection connection, String sql) throws Exception {
        for (String statementSql : statementParser.splitStatements(sql)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(statementSql);
            }
        }
    }

    private void stopTemplateProcessQuietly(DbmsType dbmsType, LvmSnapshotRuntimeNode runtimeNode,
                                            String templateVersion, String runnerPassword) {
        try {
            stopTemplateProcess(dbmsType, runtimeNode, templateVersion, runnerPassword);
        } catch (Exception ignored) {
        }
    }

    private void cleanupFailedTemplate(DbmsType dbmsType, LvmSnapshotRuntimeNode runtimeNode,
                                       String scriptDbmsName, String templateVersion,
                                       String runnerPassword, boolean processStarted,
                                       boolean templateCreationRequested) {
        // 템플릿 DB 프로세스 정지와 실패한 snapshot 정리
        RuntimeException failure = null;
        if (processStarted) {
            failure = captureFailure(failure, () -> stopTemplateProcess(
                    dbmsType, runtimeNode, templateVersion, runnerPassword));
        }
        if (templateCreationRequested) {
            failure = captureFailure(failure, () -> dropTemplate(scriptDbmsName, templateVersion));
        }

        if (failure != null) {
            throw failure;
        }
    }

    private String mysqlRootPassword(LvmSnapshotRuntimeNode runtimeNode, String runnerPassword) {
        return !runtimeNode.getRootPassword().isBlank() ? runtimeNode.getRootPassword() : runnerPassword;
    }

    private void executeCommands(List<List<String>> commands) {
        for (List<String> command : commands) {
            commandExecutor.execute(command);
        }
    }

    private RuntimeException captureFailure(RuntimeException failure, CleanupAction action) {
        // 첫 정리 실패에 이후 정리 실패를 suppressed로 누적
        try {
            action.run();
            return failure;
        } catch (RuntimeException exception) {
            if (failure == null) {
                return exception;
            }

            failure.addSuppressed(exception);
            return failure;
        }
    }

    private void sleepBeforeRetry() {
        // 템플릿 DB 준비 상태 재시도 간격 대기
        try {
            Thread.sleep(500L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LVM 스냅샷 템플릿 DB 프로세스 대기 중 중단", exception);
        }
    }

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (Exception ignored) {
        }
    }

    private interface CleanupAction {

        void run();
    }

}
