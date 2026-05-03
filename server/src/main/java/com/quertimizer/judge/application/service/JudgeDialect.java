package com.quertimizer.judge.application.service;

import java.util.List;

public interface JudgeDialect {

    String quoteIdentifier(String identifier);

    String createEnvironmentSql(String environmentName);

    List<String> useEnvironmentSqls(String environmentName);

    String dropEnvironmentIfExistsSql(String environmentName);

    List<String> statementTimeoutSqls(int timeoutSeconds);

    List<String> initializeStatisticsSqls(String environmentName);

    default String tableNamesSql(String environmentName) {
        return "";
    }

    default String analyzeTablesSql(List<String> tableNames) {
        return "";
    }

    String explainSql(String sql);

    String selectCountSql(String sql);

    String selectPageSql(String sql);
}
