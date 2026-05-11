package com.quertimizer.judge.adapter.out.execution;

import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.application.service.JudgeDialect;
import com.quertimizer.judge.application.service.JudgeDialectService;
import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.DatasetTemplateDefinition;
import com.quertimizer.judge.application.port.out.JudgeTemplateStorePort;
import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;
import com.quertimizer.judge.domain.model.EnvironmentPolicy;
import com.quertimizer.judge.domain.model.JudgeQueuePriority;
import com.quertimizer.judge.domain.model.JudgeQueueStatusListener;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class LvmSnapshotEnvironmentProvisioner implements RuntimeEnvironmentProvisioner {

    private final JudgeDialectService dialectService;
    private final JudgeTemplateStorePort templateStore;
    private final LvmSnapshotRuntimeOptions options;
    private final LvmSnapshotCommandExecutor commandExecutor;
    private final LvmSnapshotRuntimeCommandFactory commandFactory;
    private final LvmSnapshotRuntimeResourceManager resourceManager;
    private final ConcurrentHashMap<JudgeEnvironmentId, LvmSnapshotRuntimeInstance> instances = new ConcurrentHashMap<>();

    public LvmSnapshotEnvironmentProvisioner(RuntimeDatabaseCluster databaseCluster, JudgeDialectService dialectService,
                                             JudgeTemplateStorePort templateStore, LvmSnapshotRuntimeOptions options,
                                             LvmSnapshotCommandExecutor commandExecutor,
                                             LvmSnapshotRuntimeCommandFactory commandFactory) {
        this(dialectService, templateStore, options, commandExecutor, commandFactory,
                new LvmSnapshotRuntimeResourceManager(databaseCluster, options));
    }

    public LvmSnapshotEnvironmentProvisioner(JudgeDialectService dialectService,
                                             JudgeTemplateStorePort templateStore, LvmSnapshotRuntimeOptions options,
                                             LvmSnapshotCommandExecutor commandExecutor,
                                             LvmSnapshotRuntimeCommandFactory commandFactory,
                                             LvmSnapshotRuntimeResourceManager resourceManager) {
        this.dialectService = Objects.requireNonNull(dialectService, "필수 값이 없습니다.");
        this.templateStore = Objects.requireNonNull(templateStore, "필수 값이 없습니다.");
        this.options = Objects.requireNonNull(options, "필수 값이 없습니다.");
        this.commandExecutor = Objects.requireNonNull(commandExecutor, "필수 값이 없습니다.");
        this.commandFactory = Objects.requireNonNull(commandFactory, "필수 값이 없습니다.");
        this.resourceManager = Objects.requireNonNull(resourceManager, "필수 값이 없습니다.");
    }

    @Override
    public ProvisionedRuntimeEnvironment create(JudgeEnvironmentId environmentId, DatasetDefinition dataset,
                                                EnvironmentPolicy policy) {
        return create(environmentId, dataset, policy, JudgeQueuePriority.NORMAL, JudgeQueueStatusListener.noop());
    }

    @Override
    public ProvisionedRuntimeEnvironment create(JudgeEnvironmentId environmentId, DatasetDefinition dataset,
                                                EnvironmentPolicy policy, JudgeQueuePriority queuePriority,
                                                JudgeQueueStatusListener queueStatusListener) {
        // 실행 환경 입력 검증과 시작 로그 기록
        Objects.requireNonNull(environmentId, "필수 값이 없습니다.");
        Objects.requireNonNull(dataset, "필수 값이 없습니다.");
        Objects.requireNonNull(policy, "필수 값이 없습니다.");
        log.info(
                "lvm-snapshot 실행 환경 분리 시작 environmentId={}, datasetId={}, dbmsType={}",
                environmentId, dataset.getDatasetId(), dataset.getDbmsType()
        );

        // 실행 runner와 데이터셋 템플릿 정보 확보
        LvmSnapshotRuntimeSlot runtimeSlot = resourceManager.acquire(dataset.getDbmsType(), queuePriority, queueStatusListener);
        queueStatusListener.onWaiting(0);
        RuntimeDatabase runnerDatabase = runtimeSlot.getRunnerDatabase();
        LvmSnapshotRuntimeNode runtimeNode = options.requireNode(runnerDatabase.getId());
        DatasetTemplateDefinition templateDefinition = requireDatasetTemplate(dataset);
        String scriptDbmsName = LvmSnapshotNameSupport.scriptDbmsName(dataset.getDbmsType());
        String environmentScriptName = LvmSnapshotNameSupport.scriptName(environmentId.getValue());
        String templateVersion = templateDefinition.getTemplateVersion();
        int port = runtimeSlot.getPort();

        boolean snapshotCreated = false;
        boolean processStarted = false;
        try {
            // 읽기 전용 템플릿 기반 평가 snapshot 생성
            log.info(
                    "lvm-snapshot 평가 snapshot 생성 시작 environmentId={}, runnerId={}, templateVersion={}, port={}",
                    environmentId, runnerDatabase.getId(), templateVersion, port
            );
            queueStatusListener.onSnapshotCreating();
            createEvalSnapshot(scriptDbmsName, templateVersion, environmentScriptName);
            snapshotCreated = true;
            queueStatusListener.onSnapshotCreated();
            log.info(
                    "lvm-snapshot 평가 snapshot 생성 완료 environmentId={}, runnerId={}, environmentScriptName={}",
                    environmentId, runnerDatabase.getId(), environmentScriptName
            );

            // 평가 snapshot을 바라보는 DB 프로세스 시작
            log.info(
                    "lvm-snapshot 평가 DB 프로세스 시작 environmentId={}, runnerContainer={}, port={}",
                    environmentId, runtimeNode.getRunnerContainer(), port
            );
            queueStatusListener.onProcessStarting(dataset.getDbmsType());
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
            queueStatusListener.onProcessStarted(dataset.getDbmsType());
            log.info(
                    "lvm-snapshot 실행 환경 분리 완료 environmentId={}, datasetId={}, runnerId={}, port={}",
                    environmentId, dataset.getDatasetId(), runnerDatabase.getId(), port
            );

            // 생성된 실행 환경 인스턴스 등록 후 반환
            instances.put(environmentId, new LvmSnapshotRuntimeInstance(
                    runtimeSlot, runtimeNode,
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
                cleanupFailedCreation(runtimeSlot, runtimeNode, scriptDbmsName, environmentScriptName, port,
                        processStarted, snapshotCreated, runnerDatabase.getPassword());
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw new IllegalStateException("LVM 스냅샷 런타임 환경 생성 실패", exception);
        }
    }

    private DatasetTemplateDefinition requireDatasetTemplate(DatasetDefinition dataset) {
        DatasetTemplateDefinition templateDefinition = templateStore.findDatasetTemplate(dataset.getDatasetId())
                .orElseThrow(() -> new IllegalStateException("봉인된 템플릿이 등록되지 않았습니다: " + dataset.getDatasetId()));
        if (templateDefinition.getDbmsType() != dataset.getDbmsType()) {
            throw new IllegalStateException("데이터셋 템플릿 DBMS 유형이 데이터셋과 다릅니다: " + dataset.getDatasetId());
        }

        return templateDefinition;
    }

    @Override
    public RuntimeEnvironmentConnection openConnection(ProvisionedRuntimeEnvironment environment, int timeoutSeconds) {
        // 실행 환경 연결 대상 조회와 시작 로그 기록
        Objects.requireNonNull(environment, "필수 값이 없습니다.");

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

            JudgeDialect dialect = dialectService.get(runtimeEnvironment.getDatabase().getDbmsType());
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
            throw new IllegalStateException("LVM 스냅샷 런타임 환경 연결 실패", exception);
        }
    }

    @Override
    public void drop(ProvisionedRuntimeEnvironment environment) {
        // 정리 대상 실행 환경 인스턴스 조회
        Objects.requireNonNull(environment, "필수 값이 없습니다.");

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

                JudgeDialect dialect = dialectService.get(environment.getDatabase().getDbmsType());
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

        throw new IllegalStateException("LVM 스냅샷 DB 프로세스 준비 실패", lastFailure);
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

    private void cleanupFailedCreation(LvmSnapshotRuntimeSlot runtimeSlot, LvmSnapshotRuntimeNode runtimeNode,
                                       String scriptDbmsName, String environmentScriptName, int port,
                                       boolean processStarted, boolean snapshotCreated, String runnerPassword) {
        RuntimeException failure = null;
        LvmSnapshotRuntimeInstance instance = new LvmSnapshotRuntimeInstance(
                runtimeSlot, runtimeNode,
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
        resourceManager.release(instance.runtimeSlot);
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(500L);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("LVM 스냅샷 DB 프로세스 대기 중 중단", exception);
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
        private final LvmSnapshotRuntimeSlot runtimeSlot;
        private final LvmSnapshotRuntimeNode runtimeNode;
        private final String scriptDbmsName;
        private final String environmentScriptName;
        private final int port;
        private final String runnerPassword;

        private LvmSnapshotRuntimeInstance(LvmSnapshotRuntimeSlot runtimeSlot, LvmSnapshotRuntimeNode runtimeNode,
                                           String scriptDbmsName, String environmentScriptName,
                                           int port, String runnerPassword) {
            this.runtimeSlot = Objects.requireNonNull(runtimeSlot, "필수 값이 없습니다.");
            this.runtimeNode = Objects.requireNonNull(runtimeNode, "필수 값이 없습니다.");
            this.scriptDbmsName = Objects.requireNonNull(scriptDbmsName, "필수 값이 없습니다.");
            this.environmentScriptName = Objects.requireNonNull(environmentScriptName, "필수 값이 없습니다.");
            this.port = port;
            this.runnerPassword = runnerPassword != null ? runnerPassword : "";
        }

        private DbmsType dbmsType() {
            return runtimeSlot.getRunnerDatabase().getDbmsType();
        }

        private String mysqlRootPassword() {
            return !runtimeNode.getRootPassword().isBlank() ? runtimeNode.getRootPassword() : runnerPassword;
        }
    }

}
