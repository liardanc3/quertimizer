package com.quertimizer.judge.application.port;

import java.util.List;

public interface DbmsSqlDialect {

    String quoteIdentifier(String identifier);

    String createSchemaIfMissingSql(String schemaName);

    String createSchemaSql(String schemaName);

    List<String> useSchemaSqls(String schemaName);

    String dropSchemaIfExistsSql(String schemaName);

    List<String> statementTimeoutSqls(int timeoutSeconds);

    String validateSelectSql(String statementName, String sql);

    String cleanupValidatedSelectSql(String statementName);

    String explainSql(String sql);

    String explainAnalyzeSql(String sql);

    String selectCountSql(String sql);

    String selectPageSql(String sql);
}
