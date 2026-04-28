package com.quertimizer.judge.application.port;

import com.quertimizer.global.constant.DbmsType;

import java.sql.Connection;

public interface DatasetProvisioningStrategy {

    void provision(Connection connection, DbmsType dbmsType, String schemaName, String ddl, String dataSql) throws Exception;
}
