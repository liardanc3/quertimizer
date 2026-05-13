package com.quertimizer.judge.application.service;

import com.quertimizer.judge.application.model.ProvisionedEnvironment;
import com.quertimizer.judge.application.model.ExecutionEnvironment;
import com.quertimizer.judge.application.model.EnvironmentConnection;
import com.quertimizer.judge.application.model.EnvironmentName;
import com.quertimizer.judge.application.model.Database;
import com.quertimizer.judge.application.model.Names;
import com.quertimizer.judge.application.model.Options;
import com.quertimizer.judge.application.model.DatabaseNode;
import com.quertimizer.judge.application.model.DatabaseSlot;
import com.quertimizer.judge.application.port.out.LvmSnapshotPort;
import com.quertimizer.judge.application.port.out.OrphanLvmSnapshotCleanupPort;
import com.quertimizer.judge.application.port.out.ContainerPort;
import com.quertimizer.judge.application.port.out.EnvironmentConnectionPort;
import com.quertimizer.judge.application.port.out.EnvironmentProvisioner;
import com.quertimizer.judge.application.port.out.TemplateRepositoryPort;
import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.DatasetTemplateDefinition;
import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;
import com.quertimizer.judge.domain.model.DbmsType;
import com.quertimizer.judge.domain.model.EnvironmentPolicy;
import com.quertimizer.judge.domain.model.QueuePriority;
import com.quertimizer.judge.domain.model.QueueStatusListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static com.quertimizer.judge.domain.model.JudgeFailReason.LVM_SNAPSHOT_ENVIRONMENT_CREATION_FAILED;
import static com.quertimizer.judge.domain.model.JudgeFailReason.SEALED_TEMPLATE_NOT_REGISTERED;
import static com.quertimizer.judge.domain.model.JudgeFailReason.TEMPLATE_DBMS_MISMATCH;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProvisionService implements EnvironmentProvisioner, OrphanLvmSnapshotCleanupPort {

    private final TemplateRepositoryPort templateRepository;
    private final Options options;
    private final ContainerPort containerPort;
    private final LvmSnapshotPort lvmSnapshotPort;
    private final EnvironmentConnectionPort environmentConnectionPort;
    private final ConcurrentHashMap<JudgeEnvironmentId, LvmSnapshotInstance> instances = new ConcurrentHashMap<>();
    private final Set<String> creatingEvalLvNames = ConcurrentHashMap.newKeySet();

    @Override
    public ProvisionedEnvironment create(JudgeEnvironmentId environmentId, DatasetDefinition dataset,
                                                EnvironmentPolicy policy) {
        return create(environmentId, dataset, policy, QueuePriority.NORMAL, QueueStatusListener.noop());
    }

    @Override
    public ProvisionedEnvironment create(JudgeEnvironmentId environmentId, DatasetDefinition dataset,
                                                EnvironmentPolicy policy, QueuePriority queuePriority,
                                                QueueStatusListener queueStatusListener) {
        // 실행 환경 시작 로그 기록
        log.info(
                "lvm-snapshot 실행 환경 분리 시작 environmentId={}, datasetId={}, dbmsType={}",
                environmentId, dataset.getDatasetId(), dataset.getDbmsType()
        );

        // 실행 DB 노드와 데이터셋 템플릿 context 생성
        DatabaseSlot databaseSlot = lvmSnapshotPort.acquire(dataset.getDbmsType(), queuePriority, queueStatusListener);
        queueStatusListener.onWaiting(0);
        LvmSnapshotCreationContext context = createCreationContext(environmentId, dataset, databaseSlot);

        // snapshot과 DB process 생성 상태 기준 실패 보상 처리
        boolean snapshotCreated = false;
        boolean processStarted = false;
        creatingEvalLvNames.add(context.evalLvName);
        try {
            // 읽기 전용 템플릿 기반 평가 snapshot 생성
            log.info(
                    "lvm-snapshot 평가 snapshot 생성 시작 environmentId={}, databaseId={}, templateVersion={}, port={}",
                    environmentId, context.database.getId(), context.templateVersion, context.port
            );
            queueStatusListener.onSnapshotCreating();
            createEvalSnapshot(context.scriptDbmsName, context.templateVersion, context.environmentScriptName);
            snapshotCreated = true;
            queueStatusListener.onSnapshotCreated();
            log.info(
                    "lvm-snapshot 평가 snapshot 생성 완료 environmentId={}, databaseId={}, environmentScriptName={}",
                    environmentId, context.database.getId(), context.environmentScriptName
            );

            // 평가 snapshot을 바라보는 DB 프로세스 시작
            log.info(
                    "lvm-snapshot 평가 DB 프로세스 시작 environmentId={}, containerName={}, port={}",
                    environmentId, context.databaseNode.getContainerName(), context.port
            );
            queueStatusListener.onProcessStarting(dataset.getDbmsType());
            startDatabaseProcess(dataset.getDbmsType(), context.databaseNode, context.environmentScriptName, context.port);
            processStarted = true;

            // JDBC 접속 가능한 평가 DB 정보 생성과 준비 대기
            ExecutionEnvironment environment = createExecutionEnvironment(environmentId, dataset, context);
            log.info(
                    "lvm-snapshot 평가 DB 준비 대기 시작 environmentId={}, jdbcUrl={}",
                    environmentId, environment.getDatabase().getJdbcUrl()
            );
            environmentConnectionPort.waitUntilReady(environment, options.getStartupTimeoutSeconds());
            queueStatusListener.onProcessStarted(dataset.getDbmsType());
            log.info(
                    "lvm-snapshot 실행 환경 분리 완료 environmentId={}, datasetId={}, databaseId={}, port={}",
                    environmentId, dataset.getDatasetId(), context.database.getId(), context.port
            );

            // 생성된 실행 환경 인스턴스 등록 후 반환
            instances.put(environmentId, new LvmSnapshotInstance(
                    databaseSlot, context.databaseNode,
                    context.scriptDbmsName, context.environmentScriptName,
                    context.port, context.database.getPassword()
            ));
            return new ProvisionedEnvironment(environment);
        } catch (Exception exception) {
            // 실행 환경 생성 실패 시 생성된 프로세스와 snapshot 정리
            log.warn(
                    "lvm-snapshot 실행 환경 분리 실패 environmentId={}, datasetId={}, databaseId={}",
                    environmentId, dataset.getDatasetId(), context.database.getId(), exception
            );
            try {
                cleanupFailedCreation(context, processStarted, snapshotCreated);
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw new IllegalStateException(LVM_SNAPSHOT_ENVIRONMENT_CREATION_FAILED.getMessage(), exception);
        } finally {
            creatingEvalLvNames.remove(context.evalLvName);
        }
    }

    private DatasetTemplateDefinition requireDatasetTemplate(DatasetDefinition dataset) {
        DatasetTemplateDefinition templateDefinition = templateRepository.findDatasetTemplate(dataset.getDatasetId())
                .orElseThrow(() -> new IllegalStateException(SEALED_TEMPLATE_NOT_REGISTERED.format(dataset.getDatasetId())));
        if (templateDefinition.getDbmsType() != dataset.getDbmsType()) {
            throw new IllegalStateException(TEMPLATE_DBMS_MISMATCH.format(dataset.getDatasetId()));
        }

        return templateDefinition;
    }

    @Override
    public EnvironmentConnection openConnection(ProvisionedEnvironment environment, int timeoutSeconds) {
        // 실행 환경 JDBC 연결 위임
        return environmentConnectionPort.open(environment.getExecutionEnvironment(), timeoutSeconds);
    }

    @Override
    public void drop(ProvisionedEnvironment environment) {
        // 정리 대상 실행 환경 인스턴스 조회
        JudgeEnvironmentId environmentId = environment.getExecutionEnvironment().getEnvironmentId();
        LvmSnapshotInstance instance = instances.remove(environmentId);
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

    @Override
    public void cleanupOrphans() {
        // 현재 JVM이 관리 중인 평가 LV 이름 수집
        Set<String> activeEvalLvNames = instances.values().stream()
                .map(instance -> lvmSnapshotPort.evalLvName(instance.scriptDbmsName, instance.environmentScriptName))
                .collect(java.util.stream.Collectors.toCollection(HashSet::new));
        activeEvalLvNames.addAll(creatingEvalLvNames);

        // LVM VG에서 고아 평가 LV 조회 후 active 대상 제외 정리
        String output = lvmSnapshotPort.listEvalSnapshotNames();
        Arrays.stream(output.split("\\R"))
                .map(String::trim)
                .filter(lvName -> !lvName.isBlank())
                .filter(lvName -> !activeEvalLvNames.contains(lvName))
                .forEach(this::dropOrphanEvalSnapshot);
    }

    private void createEvalSnapshot(String scriptDbmsName, String templateVersion, String environmentScriptName) {
        lvmSnapshotPort.createEvalSnapshot(scriptDbmsName, templateVersion, environmentScriptName);
    }

    private void startDatabaseProcess(DbmsType dbmsType, DatabaseNode databaseNode,
                                      String environmentScriptName, int port) {
        containerPort.startEvalProcess(dbmsType, databaseNode.getContainerName(), environmentScriptName, port);
    }

    private void stopDatabaseProcess(LvmSnapshotInstance instance) {
        containerPort.stopEvalProcess(
                instance.dbmsType(), instance.databaseNode.getContainerName(),
                instance.environmentScriptName, instance.mysqlRootPassword());
    }

    private void dropEvalSnapshot(String scriptDbmsName, String environmentScriptName) {
        lvmSnapshotPort.dropEvalSnapshot(scriptDbmsName, environmentScriptName);
    }

    private void dropOrphanEvalSnapshot(String evalLvName) {
        // 고아 평가 LV 정리 실패가 batch 전체를 중단시키지 않도록 개별 처리
        try {
            log.info("lvm-snapshot 고아 평가 snapshot 정리 시작 lvName={}", evalLvName);
            containerPort.stopOrphanEvalProcesses(
                    lvmSnapshotPort.resolveEvalDbmsType(evalLvName), options.getNodes(),
                    lvmSnapshotPort.resolveEvalEnvironmentName(evalLvName)
            );
            lvmSnapshotPort.dropOrphanEvalSnapshot(evalLvName);
            log.info("lvm-snapshot 고아 평가 snapshot 정리 완료 lvName={}", evalLvName);
        } catch (Exception exception) {
            log.warn("lvm-snapshot 고아 평가 snapshot 정리 실패 lvName={}", evalLvName, exception);
        }
    }

    private LvmSnapshotCreationContext createCreationContext(JudgeEnvironmentId environmentId, DatasetDefinition dataset,
                                                            DatabaseSlot databaseSlot) {
        // 실행 환경 생성에 필요한 DB 노드, template, script 이름 묶음 구성
        Database database = databaseSlot.getDatabase();
        DatabaseNode databaseNode = options.requireNode(database.getId());
        DatasetTemplateDefinition templateDefinition = requireDatasetTemplate(dataset);
        String scriptDbmsName = Names.scriptDbmsName(dataset.getDbmsType());
        String environmentScriptName = Names.scriptName(environmentId.getValue());
        return new LvmSnapshotCreationContext(
                databaseSlot, database, databaseNode, templateDefinition,
                scriptDbmsName, environmentScriptName,
                lvmSnapshotPort.evalLvName(scriptDbmsName, environmentScriptName),
                templateDefinition.getTemplateVersion(), databaseSlot.getPort()
        );
    }

    private ExecutionEnvironment createExecutionEnvironment(JudgeEnvironmentId environmentId, DatasetDefinition dataset,
                                                        LvmSnapshotCreationContext context) {
        // 평가 DB 접속 정보와 실행 환경 생성
        return new ExecutionEnvironment(
                environmentId, dataset.getDatasetId(),
                createEvaluationDatabase(
                        context.database, context.databaseNode,
                        context.environmentScriptName, context.port
                ),
                new EnvironmentName(context.templateDefinition.getEnvironmentName()),
                Instant.now()
        );
    }

    private Database createEvaluationDatabase(Database database, DatabaseNode databaseNode,
                                                     String environmentScriptName, int port) {
        // LVM 평가 process에 접속할 DB 접속 정보 생성
        return new Database(
                database.getId() + "-" + environmentScriptName,
                database.getName() + "-" + environmentScriptName, database.getDbmsType(),
                databaseNode.createJdbcUrl(database.getDbmsType(), port),
                database.getUsername(), database.getPassword(),
                true, 1, database.getWeight()
        );
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

    private void cleanupFailedCreation(LvmSnapshotCreationContext context,
                                       boolean processStarted, boolean snapshotCreated) {
        // 실패한 실행 환경의 DB process와 snapshot 정리
        RuntimeException failure = null;
        LvmSnapshotInstance instance = new LvmSnapshotInstance(
                context.databaseSlot, context.databaseNode,
                context.scriptDbmsName, context.environmentScriptName,
                context.port, context.database.getPassword()
        );
        if (processStarted) {
            failure = captureFailure(failure, () -> stopDatabaseProcess(instance));
        }
        if (snapshotCreated) {
            failure = captureFailure(failure, () -> dropEvalSnapshot(context.scriptDbmsName, context.environmentScriptName));
        }

        // DB slot 반납과 정리 실패 전달
        releaseInstance(instance);
        if (failure != null) {
            throw failure;
        }
    }

    private void releaseInstance(LvmSnapshotInstance instance) {
        // LVM DB slot 반납
        lvmSnapshotPort.release(instance.databaseSlot);
    }

    private interface CleanupAction {
        void run();
    }

    private static final class LvmSnapshotCreationContext {
        private final DatabaseSlot databaseSlot;
        private final Database database;
        private final DatabaseNode databaseNode;
        private final DatasetTemplateDefinition templateDefinition;
        private final String scriptDbmsName;
        private final String environmentScriptName;
        private final String evalLvName;
        private final String templateVersion;
        private final int port;

        private LvmSnapshotCreationContext(DatabaseSlot databaseSlot, Database database, DatabaseNode databaseNode,
                                           DatasetTemplateDefinition templateDefinition, String scriptDbmsName,
                                           String environmentScriptName, String evalLvName,
                                           String templateVersion, int port) {
            this.databaseSlot = databaseSlot;
            this.database = database;
            this.databaseNode = databaseNode;
            this.templateDefinition = templateDefinition;
            this.scriptDbmsName = scriptDbmsName;
            this.environmentScriptName = environmentScriptName;
            this.evalLvName = evalLvName;
            this.templateVersion = templateVersion;
            this.port = port;
        }
    }

    private static final class LvmSnapshotInstance {
        private final DatabaseSlot databaseSlot;
        private final DatabaseNode databaseNode;
        private final String scriptDbmsName;
        private final String environmentScriptName;
        private final int port;
        private final String databasePassword;

        private LvmSnapshotInstance(DatabaseSlot databaseSlot, DatabaseNode databaseNode,
                                           String scriptDbmsName, String environmentScriptName,
                                           int port, String databasePassword) {
            this.databaseSlot = databaseSlot;
            this.databaseNode = databaseNode;
            this.scriptDbmsName = scriptDbmsName;
            this.environmentScriptName = environmentScriptName;
            this.port = port;
            this.databasePassword = databasePassword != null ? databasePassword : "";
        }

        private DbmsType dbmsType() {
            return databaseSlot.getDatabase().getDbmsType();
        }

        private String mysqlRootPassword() {
            return databaseNode.resolveRootPassword(databasePassword);
        }
    }

}
