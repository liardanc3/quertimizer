package com.quertimizer.judge.application.port.out;

import com.quertimizer.judge.domain.model.ExecutionMode;

import java.util.List;

public interface SqlDialect {

    String quoteIdentifier(String identifier);

    String createEnvironmentSql(String environmentName);

    List<String> useEnvironmentSqls(String environmentName);

    String dropEnvironmentIfExistsSql(String environmentName);

    List<String> statementTimeoutSqls(int timeoutSeconds);

    List<String> initializeStatisticsSqls(String environmentName);

    default String tableNamesSql(String environmentName) {
        return "";
    }

    default List<String> persistentStatisticsSqls(List<String> tableNames) {
        return List.of();
    }

    default String analyzeTablesSql(List<String> tableNames) {
        return "";
    }

    String explainSql(String sql);

    default String planSql(String sql, ExecutionMode mode) {
        return sql;
    }

    String selectCountSql(String sql);

    String selectPageSql(String sql);
}
