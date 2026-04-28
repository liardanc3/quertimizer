package com.quertimizer.judge.infrastructure.execution;

import com.quertimizer.global.constant.DbmsType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Connection;

@Component
@RequiredArgsConstructor
public class TemplateSchemaCopyProvisioningStrategy implements DatasetProvisioningStrategy {

    private final SqlReplayProvisioningStrategy sqlReplayProvisioningStrategy;

    @Override
    public void provision(Connection connection, DbmsType dbmsType, String schemaName, String ddl, String dataSql) throws Exception {
        // worker-local template cache 복제 전까지 canonical SQL replay로 dataset을 준비
        sqlReplayProvisioningStrategy.provision(connection, dbmsType, schemaName, ddl, dataSql);
    }
}
