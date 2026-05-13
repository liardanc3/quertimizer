package com.quertimizer.judge.application.service;

import com.quertimizer.judge.application.model.Database;
import com.quertimizer.judge.application.model.Names;
import com.quertimizer.judge.application.model.DatabaseNode;
import com.quertimizer.judge.application.model.DatabaseSlot;
import com.quertimizer.judge.application.model.SqlExecutorTicket;
import com.quertimizer.judge.application.port.out.LvmSnapshotPort;
import com.quertimizer.judge.application.port.out.ContainerPort;
import com.quertimizer.judge.application.port.out.DatasetLoaderPort;
import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.DatasetTemplateDefinition;
import com.quertimizer.judge.domain.model.DbmsType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

import static com.quertimizer.judge.domain.model.JudgeFailReason.DATASET_TEMPLATE_PREPARATION_FAILED;

@Slf4j
@Service
@RequiredArgsConstructor
public class DatasetTemplateService {

    private final DatabaseNodeService databaseNodeService;
    private final LvmSnapshotPort lvmSnapshotPort;
    private final ContainerPort containerPort;
    private final DatasetLoaderPort datasetLoaderPort;

    public DatasetTemplateDefinition createDatasetTemplate(DatasetDefinition dataset, SqlExecutorTicket ticket) {
        // LVM snapshot DB slot 점유와 데이터셋 템플릿 생성
        DatabaseSlot databaseSlot = lvmSnapshotPort.acquire(
                dataset.getDbmsType(), ticket.getPriority(), ticket.getStatusListener()
        );
        try {
            return prepareTemplate(dataset, databaseSlot);
        } finally {
            lvmSnapshotPort.release(databaseSlot);
        }
    }

    public void dropDatasetTemplate(DatasetTemplateDefinition templateDefinition) {
        // 봉인된 LVM 템플릿 snapshot 제거
        String scriptDbmsName = Names.scriptDbmsName(templateDefinition.getDbmsType());
        log.info(
                "lvm-snapshot 데이터셋 템플릿 제거 시작 datasetId={}, templateVersion={}",
                templateDefinition.getDatasetId().getValue(), templateDefinition.getTemplateVersion()
        );
        lvmSnapshotPort.dropTemplate(scriptDbmsName, templateDefinition.getTemplateVersion());
        log.info(
                "lvm-snapshot 데이터셋 템플릿 제거 완료 datasetId={}, templateVersion={}",
                templateDefinition.getDatasetId().getValue(), templateDefinition.getTemplateVersion()
        );
    }

    private DatasetTemplateDefinition prepareTemplate(DatasetDefinition dataset, DatabaseSlot databaseSlot) {
        // 템플릿 생성용 DB 노드와 snapshot 이름 정보 확보
        log.info(
                "lvm-snapshot 데이터셋 템플릿 준비 시작 datasetId={}, dbmsType={}",
                dataset.getDatasetId().getValue(), dataset.getDbmsType()
        );
        Database database = databaseSlot.getDatabase();
        DatabaseNode databaseNode = databaseNodeService.requireNode(database.getId());
        String scriptDbmsName = Names.scriptDbmsName(dataset.getDbmsType());
        String templateVersion = Names.scriptName(dataset.getDatasetId().getValue());
        String environmentName = Names.datasetEnvironmentName(dataset.getDatasetId().getValue());
        int port = databaseSlot.getPort();

        boolean templateCreationRequested = false;
        boolean processStarted = false;
        try {
            // 유지보수 템플릿 snapshot 생성과 템플릿 DB 프로세스 시작
            templateCreationRequested = true;
            lvmSnapshotPort.createMaintenanceTemplate(scriptDbmsName, templateVersion);
            lvmSnapshotPort.prepareTemplateLog(scriptDbmsName, templateVersion);
            containerPort.startTemplateProcess(dataset.getDbmsType(), databaseNode.getContainerName(), templateVersion, port);
            processStarted = true;

            // 템플릿 DB 준비 대기와 데이터셋 적재
            Database templateDatabase = createTemplateDatabase(database, databaseNode, templateVersion, port);
            datasetLoaderPort.waitUntilReady(templateDatabase, databaseNodeService.startupTimeoutSeconds());
            datasetLoaderPort.load(templateDatabase, environmentName, dataset);

            // 템플릿 DB 프로세스 정지와 snapshot 봉인
            containerPort.stopTemplateProcess(
                    dataset.getDbmsType(), databaseNode.getContainerName(),
                    templateVersion, databaseNode.resolveRootPassword(database.getPassword())
            );
            processStarted = false;
            lvmSnapshotPort.sealTemplate(scriptDbmsName, templateVersion);
            log.info(
                    "lvm-snapshot 데이터셋 템플릿 준비 완료 datasetId={}, templateVersion={}",
                    dataset.getDatasetId().getValue(), templateVersion
            );
            return new DatasetTemplateDefinition(
                    dataset.getDatasetId(), dataset.getDbmsType(),
                    templateVersion, environmentName, Instant.now()
            );
        } catch (Exception exception) {
            // 생성 실패 시 템플릿 DB 프로세스와 snapshot 정리
            try {
                cleanupFailedTemplate(
                        dataset.getDbmsType(), databaseNode, scriptDbmsName, templateVersion,
                        database.getPassword(), processStarted, templateCreationRequested
                );
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }
            throw new IllegalStateException(DATASET_TEMPLATE_PREPARATION_FAILED.getMessage(), exception);
        }
    }

    private Database createTemplateDatabase(Database database, DatabaseNode databaseNode,
                                            String templateVersion, int port) {
        // LVM template process에 접속할 임시 DB 접속 정보 생성
        return new Database(
                database.getId() + "-template-" + templateVersion,
                database.getName() + "-template-" + templateVersion, database.getDbmsType(),
                databaseNode.createJdbcUrl(database.getDbmsType(), port),
                database.getUsername(), database.getPassword(),
                true, 1, database.getWeight()
        );
    }

    private void cleanupFailedTemplate(DbmsType dbmsType, DatabaseNode databaseNode,
                                       String scriptDbmsName, String templateVersion,
                                       String databasePassword, boolean processStarted,
                                       boolean templateCreationRequested) {
        // 템플릿 DB 프로세스 정지와 실패 snapshot 정리
        RuntimeException failure = null;
        if (processStarted) {
            failure = captureFailure(failure, () -> containerPort.stopTemplateProcess(
                    dbmsType, databaseNode.getContainerName(),
                    templateVersion, databaseNode.resolveRootPassword(databasePassword))
            );
        }
        if (templateCreationRequested) {
            failure = captureFailure(failure, () -> lvmSnapshotPort.dropTemplate(scriptDbmsName, templateVersion));
        }

        // 정리 실패가 있으면 호출자에게 전달
        if (failure != null) {
            throw failure;
        }
    }

    private RuntimeException captureFailure(RuntimeException failure, Runnable action) {
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
}
