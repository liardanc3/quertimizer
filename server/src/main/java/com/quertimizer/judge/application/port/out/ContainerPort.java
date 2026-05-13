package com.quertimizer.judge.application.port.out;

import com.quertimizer.judge.application.model.DatabaseNode;
import com.quertimizer.judge.domain.model.DbmsType;

import java.util.List;

public interface ContainerPort {

    void startEvalProcess(DbmsType dbmsType, String containerName, String environmentId, int port);

    void stopEvalProcess(DbmsType dbmsType, String containerName, String environmentId, String mysqlRootPassword);

    void startTemplateProcess(DbmsType dbmsType, String containerName, String templateVersion, int port);

    void stopTemplateProcess(DbmsType dbmsType, String containerName, String templateVersion, String mysqlRootPassword);

    void stopOrphanEvalProcesses(DbmsType dbmsType, List<DatabaseNode> databaseNodes, String environmentId);
}
