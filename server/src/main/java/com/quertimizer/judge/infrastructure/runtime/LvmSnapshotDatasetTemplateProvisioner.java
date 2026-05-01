package com.quertimizer.judge.infrastructure.runtime;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.infrastructure.dialect.JudgeDialect;
import com.quertimizer.judge.infrastructure.dialect.JudgeDialectProvider;
import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.DatasetTemplateDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LvmSnapshotDatasetTemplateProvisioner implements DatasetTemplateProvisioner {

    private static final Logger log = LoggerFactory.getLogger(LvmSnapshotDatasetTemplateProvisioner.class);

    private final RuntimeDatabaseCluster databaseCluster;
    private final JudgeDialectProvider dialectProvider;
    private final LvmSnapshotRuntimeOptions options;
    private final LvmSnapshotCommandExecutor commandExecutor;
    private final LvmSnapshotRuntimeCommandFactory commandFactory;
    private final SqlStatementParser statementParser;
    private final RuntimeStatisticsInitializer statisticsInitializer;
    private final Map<String, RuntimeDatabasePool> runnerPools;
    private final Map<String, PortPool> portPools;
    private final ConcurrentHashMap<DbmsType, AtomicInteger> selectionIndexes = new ConcurrentHashMap<>();

    public LvmSnapshotDatasetTemplateProvisioner(RuntimeDatabaseCluster databaseCluster,
                                                 JudgeDialectProvider dialectProvider,
                                                 LvmSnapshotRuntimeOptions options,
                                                 LvmSnapshotCommandExecutor commandExecutor,
                                                 LvmSnapshotRuntimeCommandFactory commandFactory,
                                                 SqlStatementParser statementParser) {
        this.databaseCluster = Objects.requireNonNull(databaseCluster, "databaseCluster must not be null");
        this.dialectProvider = Objects.requireNonNull(dialectProvider, "dialectProvider must not be null");
        this.options = Objects.requireNonNull(options, "options must not be null");
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor must not be null");
        this.commandFactory = Objects.requireNonNull(commandFactory, "commandFactory must not be null");
        this.statementParser = Objects.requireNonNull(statementParser, "statementParser must not be null");
        this.statisticsInitializer = new RuntimeStatisticsInitializer();
        this.runnerPools = createRunnerPools(databaseCluster.getConfiguredDatabases());
        this.portPools = createPortPools(this.runnerPools.keySet());
    }

    @Override
    public Optional<DatasetTemplateDefinition> prepare(DatasetDefinition dataset) {
        // 데이터셋 템플릿 준비 입력 검증과 시작 로그 기록
        Objects.requireNonNull(dataset, "dataset must not be null");
        log.info(
                "lvm-snapshot 데이터셋 템플릿 준비 시작 datasetId={}, dbmsType={}",
                dataset.getDatasetId().getValue(), dataset.getDbmsType()
        );

        // 템플릿 생성용 runner와 snapshot 이름 정보 확보
        RuntimeDatabaseLease runnerLease = acquireRunner(dataset.getDbmsType());
        RuntimeDatabase runnerDatabase = runnerLease.getDatabase();
        LvmSnapshotRuntimeNode runtimeNode = options.requireNode(runnerDatabase.getId());
        String scriptDbmsName = LvmSnapshotNameSupport.scriptDbmsName(dataset.getDbmsType());
        String templateVersion = LvmSnapshotNameSupport.scriptName(dataset.getDatasetId().getValue());
        RuntimeEnvironmentName environmentName = LvmSnapshotNameSupport.datasetEnvironmentName(dataset.getDatasetId().getValue());
        int port = portPools.get(runnerDatabase.getId()).acquire();

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
            throw new IllegalStateException("lvm-snapshot dataset template preparation failed", exception);
        } finally {
            // 템플릿 준비용 포트와 runner lease 반환
            portPools.get(runnerDatabase.getId()).release(port);
            runnerLease.close();
        }
    }

    private RuntimeDatabaseLease acquireRunner(DbmsType dbmsType) {
        List<RuntimeDatabase> candidates = databaseCluster.getConfiguredDatabases().stream()
                .filter(RuntimeDatabase::isEnabled)
                .filter(database -> database.getDbmsType() == dbmsType)
                .filter(database -> options.findNode(database.getId()).isPresent())
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("No LVM snapshot runner is configured for " + dbmsType);
        }

        AtomicInteger selectionIndex = selectionIndexes.computeIfAbsent(dbmsType, ignored -> new AtomicInteger());
        int startIndex = Math.floorMod(selectionIndex.getAndIncrement(), candidates.size());
        for (int offset = 0; offset < candidates.size(); offset++) {
            RuntimeDatabase candidate = candidates.get(Math.floorMod(startIndex + offset, candidates.size()));
            RuntimeDatabasePool pool = runnerPools.get(candidate.getId());
            if (pool.hasAvailableLease()) {
                return pool.acquire();
            }
        }

        throw new IllegalStateException("No LVM snapshot runner lease is available for " + dbmsType);
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
        JudgeDialect dialect = dialectProvider.get(templateDatabase.getDbmsType());
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

        throw new IllegalStateException("lvm-snapshot template database process did not become ready", lastFailure);
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

    private Map<String, RuntimeDatabasePool> createRunnerPools(List<RuntimeDatabase> databases) {
        Map<String, RuntimeDatabasePool> pools = new LinkedHashMap<>();
        for (RuntimeDatabase database : Objects.requireNonNull(databases, "databases must not be null")) {
            if (database.isEnabled() && options.findNode(database.getId()).isPresent()) {
                pools.put(database.getId(), new RuntimeDatabasePool(database));
            }
        }

        return Map.copyOf(pools);
    }

    private Map<String, PortPool> createPortPools(Iterable<String> databaseIds) {
        Map<String, PortPool> createdPortPools = new LinkedHashMap<>();
        for (String databaseId : databaseIds) {
            LvmSnapshotRuntimeNode runtimeNode = options.requireNode(databaseId);
            createdPortPools.put(databaseId, new PortPool(runtimeNode.getPortStart(), runtimeNode.getPortEnd()));
        }

        return Map.copyOf(createdPortPools);
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
            throw new IllegalStateException("Interrupted while waiting for lvm-snapshot template database process", exception);
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

    private static final class PortPool {
        private final int portStart;
        private final int portEnd;
        private final Deque<Integer> availablePorts = new ArrayDeque<>();

        private PortPool(int portStart, int portEnd) {
            this.portStart = portStart;
            this.portEnd = portEnd;
            for (int port = portStart; port <= portEnd; port++) {
                availablePorts.addLast(port);
            }
        }

        private synchronized int acquire() {
            Integer port = availablePorts.pollFirst();
            if (port == null) {
                throw new IllegalStateException("No LVM snapshot template port is available");
            }

            return port;
        }

        private synchronized void release(int port) {
            if (port < portStart || port > portEnd || availablePorts.contains(port)) {
                return;
            }

            availablePorts.addLast(port);
        }
    }
}
