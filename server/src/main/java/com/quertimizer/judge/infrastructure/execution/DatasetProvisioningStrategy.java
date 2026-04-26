package com.quertimizer.judge.infrastructure.execution;

import java.sql.Connection;

public interface DatasetProvisioningStrategy {

    void provision(Connection connection, String schemaName, String ddl, String dataSql) throws Exception;
}
