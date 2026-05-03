package com.quertimizer.judge.application.service;

import java.util.List;

public class MySqlDialect implements JudgeDialect {

    @Override
    public String quoteIdentifier(String identifier) {
        return "`" + identifier.replace("`", "``") + "`";
    }

    @Override
    public String createEnvironmentSql(String environmentName) {
        return "CREATE SCHEMA " + quoteIdentifier(environmentName);
    }

    @Override
    public List<String> useEnvironmentSqls(String environmentName) {
        return List.of("USE " + quoteIdentifier(environmentName));
    }

    @Override
    public String dropEnvironmentIfExistsSql(String environmentName) {
        return "DROP SCHEMA IF EXISTS " + quoteIdentifier(environmentName);
    }

    @Override
    public List<String> statementTimeoutSqls(int timeoutSeconds) {
        return List.of("SET SESSION MAX_EXECUTION_TIME = " + timeoutSeconds * 1000L);
    }

    @Override
    public List<String> initializeStatisticsSqls(String environmentName) {
        return List.of();
    }

    @Override
    public String tableNamesSql(String environmentName) {
        return "SELECT table_name FROM information_schema.tables"
                + " WHERE table_schema = " + quoteLiteral(environmentName)
                + " AND table_type = 'BASE TABLE'"
                + " ORDER BY table_name";
    }

    @Override
    public String analyzeTablesSql(List<String> tableNames) {
        if (tableNames == null || tableNames.isEmpty()) {
            return "";
        }

        return "ANALYZE TABLE " + tableNames.stream()
                .map(this::quoteIdentifier)
                .collect(java.util.stream.Collectors.joining(", "));
    }

    @Override
    public String explainSql(String sql) {
        return "EXPLAIN " + sql;
    }

    @Override
    public String selectCountSql(String sql) {
        return "SELECT COUNT(*) FROM (" + sql + ") judge_result_count";
    }

    @Override
    public String selectPageSql(String sql) {
        return "SELECT * FROM (" + sql + ") judge_result_page LIMIT ? OFFSET ?";
    }

    private String quoteLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
