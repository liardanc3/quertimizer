package com.quertimizer.judge.adapter.out.jdbc.dialect;

import com.quertimizer.judge.application.port.out.SqlDialect;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PostgreSqlDialect implements SqlDialect {

    @Override
    public String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    @Override
    public String createEnvironmentSql(String environmentName) {
        return "CREATE SCHEMA " + quoteIdentifier(environmentName);
    }

    @Override
    public List<String> useEnvironmentSqls(String environmentName) {
        return List.of("SET LOCAL search_path TO " + quoteIdentifier(environmentName) + ", public");
    }

    @Override
    public String dropEnvironmentIfExistsSql(String environmentName) {
        return "DROP SCHEMA IF EXISTS " + quoteIdentifier(environmentName) + " CASCADE";
    }

    @Override
    public List<String> statementTimeoutSqls(int timeoutSeconds) {
        return List.of("SET LOCAL statement_timeout TO '" + timeoutSeconds + "s'");
    }

    @Override
    public List<String> initializeStatisticsSqls(String environmentName) {
        return List.of("ANALYZE");
    }

    @Override
    public String explainSql(String sql) {
        return "EXPLAIN " + sql;
    }

    @Override
    public String selectCountSql(String sql) {
        return "SELECT COUNT(*) FROM (" + sql + ") result_count";
    }

    @Override
    public String selectPageSql(String sql) {
        return "SELECT * FROM (" + sql + ") result_page LIMIT ? OFFSET ?";
    }
}
