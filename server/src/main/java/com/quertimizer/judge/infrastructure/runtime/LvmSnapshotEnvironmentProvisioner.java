package com.quertimizer.judge.infrastructure.runtime;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.infrastructure.dialect.JudgeDialect;
import com.quertimizer.judge.infrastructure.dialect.JudgeDialectProvider;
import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.DatasetTemplateDefinition;
import com.quertimizer.judge.application.port.JudgeTemplateStore;
import com.quertimizer.judge.domain.entity.ids.JudgeEnvironmentId;
import com.quertimizer.judge.domain.model.EnvironmentPolicy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class LvmSnapshotEnvironmentProvisioner implements RuntimeEnvironmentProvisioner {

    private static final Logger log = LoggerFactory.getLogger(LvmSnapshotEnvironmentProvisioner.class);

    private final RuntimeDatabaseCluster databaseCluster;
    private final JudgeDialectProvider dialectProvider;
    private final JudgeTemplateStore templateStore;
    private final LvmSnapshotRuntimeOptions options;
    private final LvmSnapshotCommandExecutor commandExecutor;
    private final LvmSnapshotRuntimeCommandFactory commandFactory;
    private final Map<String, RuntimeDatabasePool> runnerPools;
    private final Map<String, PortPool> portPools;
    private final ConcurrentHashMap<DbmsType, AtomicInteger> selectionIndexes = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<JudgeEnvironmentId, LvmSnapshotRuntimeInstance> instances = new ConcurrentHashMap<>();

    public LvmSnapshotEnvironmentProvisioner(RuntimeDatabaseCluster databaseCluster, JudgeDialectProvider dialectProvider,
                                             JudgeTemplateStore templateStore, LvmSnapshotRuntimeOptions options,
                                             LvmSnapshotCommandExecutor commandExecutor,
                                             LvmSnapshotRuntimeCommandFactory commandFactory) {
        this.databaseCluster = Objects.requireNonNull(databaseCluster, "databaseCluster must not be null");
        this.dialectProvider = Objects.requireNonNull(dialectProvider, "dialectProvider must not be null");
        this.templateStore = Objects.requireNonNull(templateStore, "templateStore must not be null");
        this.options = Objects.requireNonNull(options, "options must not be null");
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "commandExecutor must not be null");
        this.commandFactory = Objects.requireNonNull(commandFactory, "commandFactory must not be null");
        this.runnerPools = createRunnerPools(databaseCluster.getConfiguredDatabases());
        this.portPools = createPortPools(this.runnerPools.keySet());
    }

    @Override
    public ProvisionedRuntimeEnvironment create(JudgeEnvironmentId environmentId, DatasetDefinition dataset,
                                                EnvironmentPolicy policy) {
        // 실행 환경 입력 검증과 시작 로그 기록
        Objects.requireNonNull(environmentId, "environmentId must not be null");
        Objects.requireNonNull(dataset, "dataset must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        log.info(
                "lvm-snapshot 실행 환경 분리 시작 environmentId={}, datasetId={}, dbmsType={}",
                environmentId, dataset.getDatasetId(), dataset.getDbmsType()
        );

        // 실행 runner와 데이터셋 템플릿 정보 확보
        RuntimeDatabaseLease runnerLease = acquireRunner(dataset.getDbmsType());
        RuntimeDatabase runnerDatabase = runnerLease.getDatabase();
        LvmSnapshotRuntimeNode runtimeNode = options.requireNode(runnerDatabase.getId());
        DatasetTemplateDefinition templateDefinition = requireDatasetTemplate(dataset);
        String scriptDbmsName = LvmSnapshotNameSupport.scriptDbmsName(dataset.getDbmsType());
        String environmentScriptName = LvmSnapshotNameSupport.scriptName(environmentId.getValue());
        String templateVersion = templateDefinition.getTemplateVersion();
        int port = portPools.get(runnerDatabase.getId()).acquire();

        boolean snapshotCreated = false;
        boolean processStarted = false;
        try {
            // 읽기 전용 템플릿 기반 평가 snapshot 생성
            log.info(
                    "lvm-snapshot 평가 snapshot 생성 시작 environmentId={}, runnerId={}, templateVersion={}, port={}",
                    environmentId, runnerDatabase.getId(), templateVersion, port
            );
            createEvalSnapshot(scriptDbmsName, templateVersion, environmentScriptName);
            snapshotCreated = true;
            log.info(
                    "lvm-snapshot 평가 snapshot 생성 완료 environmentId={}, runnerId={}, environmentScriptName={}",
                    environmentId, runnerDatabase.getId(), environmentScriptName
            );

            // 평가 snapshot을 바라보는 DB 프로세스 시작
            log.info(
                    "lvm-snapshot 평가 DB 프로세스 시작 environmentId={}, runnerContainer={}, port={}",
                    environmentId, runtimeNode.getRunnerContainer(), port
            );
            startDatabaseProcess(dataset.getDbmsType(), runtimeNode, environmentScriptName, port);
            processStarted = true;

            // JDBC 접속 가능한 평가 DB 정보 생성과 준비 대기
            RuntimeDatabase evaluationDatabase = createEvaluationDatabase(runnerDatabase, runtimeNode, environmentScriptName, port);
            RuntimeEnvironment environment = new RuntimeEnvironment(
                    environmentId, dataset.getDatasetId(),
                    evaluationDatabase,
                    new RuntimeEnvironmentName(templateDefinition.getEnvironmentName()),
                    Instant.now()
            );
            log.info(
                    "lvm-snapshot 평가 DB 준비 대기 시작 environmentId={}, jdbcUrl={}",
                    environmentId, evaluationDatabase.getJdbcUrl()
            );
            waitUntilReady(environment);
            log.info(
                    "lvm-snapshot 실행 환경 분리 완료 environmentId={}, datasetId={}, runnerId={}, port={}",
                    environmentId, dataset.getDatasetId(), runnerDatabase.getId(), port
            );

            // 생성된 실행 환경 인스턴스 등록 후 반환
            instances.put(environmentId, new LvmSnapshotRuntimeInstance(
                    runnerLease, runtimeNode,
                    scriptDbmsName, environmentScriptName, port,
                    runnerDatabase.getPassword()
            ));
            return new ProvisionedRuntimeEnvironment(environment, "lvm-snapshot");
        } catch (Exception exception) {
            // 실행 환경 생성 실패 시 생성된 프로세스와 snapshot 정리
            log.warn(
                    "lvm-snapshot 실행 환경 분리 실패 environmentId={}, datasetId={}, runnerId={}",
                    environmentId, dataset.getDatasetId(), runnerDatabase.getId(), exception
            );
            try {
                cleanupFailedCreation(runnerLease, runtimeNode, scriptDbmsName, environmentScriptName, port,
                        processStarted, snapshotCreated, runnerDatabase.getPassword());
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw new IllegalStateException("lvm-snapshot runtime environment creation failed", exception);
        }
    }

    private DatasetTemplateDefinition requireDatasetTemplate(DatasetDefinition dataset) {
        DatasetTemplateDefinition templateDefinition = templateStore.findDatasetTemplate(dataset.getDatasetId())
                .orElseThrow(() -> new IllegalStateException("No sealed template is registered for " + dataset.getDatasetId()));
        if (templateDefinition.getDbmsType() != dataset.getDbmsType()) {
            throw new IllegalStateException("Dataset template DBMS type does not match dataset: " + dataset.getDatasetId());
        }

        return templateDefinition;
    }

    @Override
    public RuntimeEnvironmentConnection openConnection(ProvisionedRuntimeEnvironment environment, int timeoutSeconds) {
        // 실행 환경 연결 대상 조회와 시작 로그 기록
        Objects.requireNonNull(environment, "environment must not be null");

        RuntimeEnvironment runtimeEnvironment = environment.getRuntimeEnvironment();
        log.info(
                "lvm-snapshot 실행 환경 연결 시작 environmentId={}, databaseId={}, timeoutSeconds={}",
                runtimeEnvironment.getEnvironmentId(), runtimeEnvironment.getDatabase().getId(), timeoutSeconds
        );
        RuntimeDatabaseLease lease = null;
        Connection connection = null;
        try {
            // JDBC 연결 생성과 실행 환경 선택
            lease = new RuntimeDatabaseLease(runtimeEnvironment.getDatabase());
            connection = lease.openConnection();
            connection.setAutoCommit(false);

            JudgeDialect dialect = dialectProvider.get(runtimeEnvironment.getDatabase().getDbmsType());
            configureExecutionConnection(connection, dialect, runtimeEnvironment.getName().getValue(), timeoutSeconds);
            log.info(
                    "lvm-snapshot 실행 환경 연결 완료 environmentId={}, environmentName={}",
                    runtimeEnvironment.getEnvironmentId(), runtimeEnvironment.getName().getValue()
            );
            return new RuntimeEnvironmentConnection(lease, connection, dialect, runtimeEnvironment.getName().getValue());
        } catch (Exception exception) {
            // 연결 실패 시 열린 리소스 정리
            closeQuietly(connection);
            closeQuietly(lease);
            log.warn(
                    "lvm-snapshot 실행 환경 연결 실패 environmentId={}",
                    runtimeEnvironment.getEnvironmentId(), exception
            );
            throw new IllegalStateException("lvm-snapshot runtime environment connection failed", exception);
        }
    }

    @Override
    public void drop(ProvisionedRuntimeEnvironment environment) {
        // 정리 대상 실행 환경 인스턴스 조회
        Objects.requireNonNull(environment, "environment must not be null");

        JudgeEnvironmentId environmentId = environment.getRuntimeEnvironment().getEnvironmentId();
        LvmSnapshotRuntimeInstance instance = instances.remove(environmentId);
        if (instance == null) {
            log.info("lvm-snapshot 실행 환경 정리 대상 없음 environmentId={}", environmentId);
            return;
        }

        // DB 프로세스와 평가 snapshot 정리
        log.info(
                "lvm-snapshot 실행 환경 정리 시작 environmentId={}, environmentScriptName={}, port={}",
                environmentId, instance.environmentScriptName, instance.port
        );
        RuntimeException failure = null;
        failure = captureFailure(failure, () -> stopDatabaseProcess(instance));
        failure = captureFailure(failure, () -> dropEvalSnapshot(instance.scriptDbmsName, instance.environmentScriptName));
        releaseInstance(instance);
        if (failure != null) {
            log.warn("lvm-snapshot 실행 환경 정리 실패 environmentId={}", environmentId, failure);
            throw failure;
        }
        log.info("lvm-snapshot 실행 환경 정리 완료 environmentId={}", environmentId);
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

    private void createEvalSnapshot(String scriptDbmsName, String templateVersion, String environmentScriptName) {
        executeCommands(commandFactory.createEvalSnapshotCommands(scriptDbmsName, templateVersion, environmentScriptName));
    }

    private void startDatabaseProcess(DbmsType dbmsType, LvmSnapshotRuntimeNode runtimeNode,
                                      String environmentScriptName, int port) {
        commandExecutor.execute(commandFactory.startEvalProcessCommand(
                dbmsType, runtimeNode.getRunnerContainer(),
                environmentScriptName, port));
    }

    private void stopDatabaseProcess(LvmSnapshotRuntimeInstance instance) {
        commandExecutor.execute(commandFactory.stopEvalProcessCommand(
                instance.dbmsType(), instance.runtimeNode.getRunnerContainer(),
                instance.environmentScriptName, instance.mysqlRootPassword()));
    }

    private void dropEvalSnapshot(String scriptDbmsName, String environmentScriptName) {
        executeCommands(commandFactory.dropEvalSnapshotCommands(scriptDbmsName, environmentScriptName));
    }

    private RuntimeDatabase createEvaluationDatabase(RuntimeDatabase runnerDatabase, LvmSnapshotRuntimeNode runtimeNode,
                                                     String environmentScriptName, int port) {
        return new RuntimeDatabase(
                runnerDatabase.getId() + "-" + environmentScriptName,
                runnerDatabase.getName() + "-" + environmentScriptName,
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

    private void waitUntilReady(RuntimeEnvironment environment) {
        RuntimeDatabaseLease lease = null;
        Connection connection = null;
        Exception lastFailure = null;
        Instant deadline = Instant.now().plusSeconds(options.getStartupTimeoutSeconds());
        while (!Instant.now().isAfter(deadline)) {
            try {
                lease = new RuntimeDatabaseLease(environment.getDatabase());
                connection = lease.openConnection();
                connection.setAutoCommit(false);

                JudgeDialect dialect = dialectProvider.get(environment.getDatabase().getDbmsType());
                configureExecutionConnection(connection, dialect, environment.getName().getValue(), options.getStartupTimeoutSeconds());
                rollback(connection);
                closeQuietly(connection);
                closeQuietly(lease);
                return;
            } catch (Exception exception) {
                lastFailure = exception;
                closeQuietly(connection);
                closeQuietly(lease);
                sleepBeforeRetry();
            } finally {
                connection = null;
                lease = null;
            }
        }

        throw new IllegalStateException("lvm-snapshot database process did not become ready", lastFailure);
    }

    private void configureExecutionConnection(Connection connection, JudgeDialect dialect,
                                              String environmentName,
                                              int timeoutSeconds) throws Exception {
        try (var statement = connection.createStatement()) {
            for (String useEnvironmentSql : dialect.useEnvironmentSqls(environmentName)) {
                statement.execute(useEnvironmentSql);
            }
            for (String timeoutSql : dialect.statementTimeoutSqls(timeoutSeconds)) {
                statement.execute(timeoutSql);
            }
        }
    }

    private RuntimeException captureFailure(RuntimeException failure, CleanupAction action) {
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

    private void cleanupFailedCreation(RuntimeDatabaseLease runnerLease, LvmSnapshotRuntimeNode runtimeNode,
                                       String scriptDbmsName, String environmentScriptName, int port,
                                       boolean processStarted, boolean snapshotCreated, String runnerPassword) {
        RuntimeException failure = null;
        LvmSnapshotRuntimeInstance instance = new LvmSnapshotRuntimeInstance(
                runnerLease, runtimeNode,
                scriptDbmsName, environmentScriptName, port,
                runnerPassword
        );
        if (processStarted) {
            failure = captureFailure(failure, () -> stopDatabaseProcess(instance));
        }
        if (snapshotCreated) {
            failure = captureFailure(failure, () -> dropEvalSnapshot(scriptDbmsName, environmentScriptName));
        }

        releaseInstance(instance);
        if (failure != null) {
            throw failure;
        }
    }

    private void releaseInstance(LvmSnapshotRuntimeInstance instance) {
        portPools.get(instance.runnerLease.getDatabase().getId()).release(instance.port);
        instance.runnerLease.close();
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

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(500L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for lvm-snapshot database process", exception);
        }
    }

    private void executeCommands(List<List<String>> commands) {
        for (List<String> command : commands) {
            commandExecutor.execute(command);
        }
    }

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (Exception ignored) {
        }
    }

    private void closeQuietly(Connection connection) {
        if (connection == null) {
            return;
        }

        try {
            connection.close();
        } catch (SQLException ignored) {
        }
    }

    private void closeQuietly(RuntimeDatabaseLease lease) {
        if (lease != null) {
            lease.close();
        }
    }

    private interface CleanupAction {
        void run();
    }

    private static final class LvmSnapshotRuntimeInstance {
        private final RuntimeDatabaseLease runnerLease;
        private final LvmSnapshotRuntimeNode runtimeNode;
        private final String scriptDbmsName;
        private final String environmentScriptName;
        private final int port;
        private final String runnerPassword;

        private LvmSnapshotRuntimeInstance(RuntimeDatabaseLease runnerLease, LvmSnapshotRuntimeNode runtimeNode,
                                           String scriptDbmsName, String environmentScriptName,
                                           int port, String runnerPassword) {
            this.runnerLease = Objects.requireNonNull(runnerLease, "runnerLease must not be null");
            this.runtimeNode = Objects.requireNonNull(runtimeNode, "runtimeNode must not be null");
            this.scriptDbmsName = Objects.requireNonNull(scriptDbmsName, "scriptDbmsName must not be null");
            this.environmentScriptName = Objects.requireNonNull(environmentScriptName, "environmentScriptName must not be null");
            this.port = port;
            this.runnerPassword = runnerPassword != null ? runnerPassword : "";
        }

        private DbmsType dbmsType() {
            return runnerLease.getDatabase().getDbmsType();
        }

        private String mysqlRootPassword() {
            return !runtimeNode.getRootPassword().isBlank() ? runtimeNode.getRootPassword() : runnerPassword;
        }
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
                throw new IllegalStateException("No LVM snapshot runtime port is available");
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
