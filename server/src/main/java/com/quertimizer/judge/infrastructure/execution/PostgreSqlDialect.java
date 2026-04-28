package com.quertimizer.judge.infrastructure.execution;

import java.util.List;

public class PostgreSqlDialect implements DbmsSqlDialect {

    @Override
    public String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String createSchemaIfMissingSql(String schemaName) {
        return "CREATE SCHEMA IF NOT EXISTS " + quoteIdentifier(schemaName);
    }

    @Override
    public String createSchemaSql(String schemaName) {
        return "CREATE SCHEMA " + quoteIdentifier(schemaName);
    }

    @Override
    public List<String> useSchemaSqls(String schemaName) {
        return List.of("SET LOCAL search_path TO " + quoteIdentifier(schemaName) + ", public");
    }

    @Override
    public String dropSchemaIfExistsSql(String schemaName) {
        return "DROP SCHEMA IF EXISTS " + quoteIdentifier(schemaName) + " CASCADE";
    }

    @Override
    public List<String> statementTimeoutSqls(int timeoutSeconds) {
        return List.of("SET LOCAL statement_timeout TO '" + timeoutSeconds + "s'");
    }

    @Override
    public String validateSelectSql(String statementName, String sql) {
        return "PREPARE " + quoteIdentifier(statementName) + " AS " + sql;
    }

    @Override
    public String cleanupValidatedSelectSql(String statementName) {
        return "DEALLOCATE " + quoteIdentifier(statementName);
    }

    @Override
    public String explainSql(String sql) {
        return "EXPLAIN " + sql;
    }

    @Override
    public String explainAnalyzeSql(String sql) {
        return "EXPLAIN ANALYZE " + sql;
    }

    @Override
    public String selectCountSql(String sql) {
        return "SELECT COUNT(*) FROM (" + sql + ") execution_result_count";
    }

    @Override
    public String selectPageSql(String sql) {
        return "SELECT * FROM (" + sql + ") execution_result_page LIMIT ? OFFSET ?";
    }
}
