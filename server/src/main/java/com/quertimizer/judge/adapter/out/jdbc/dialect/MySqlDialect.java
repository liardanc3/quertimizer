package com.quertimizer.judge.adapter.out.jdbc.dialect;

import com.quertimizer.judge.application.model.Constants;
import com.quertimizer.judge.application.port.out.SqlDialect;
import com.quertimizer.judge.domain.model.ExecutionMode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class MySqlDialect implements SqlDialect {

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
        return List.of(
                "SET SESSION MAX_EXECUTION_TIME = " + timeoutSeconds * 1000L,
                "SET SESSION eq_range_index_dive_limit = 0"
        );
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
    public List<String> persistentStatisticsSqls(List<String> tableNames) {
        return tableNames.stream()
                .map(tableName -> "ALTER TABLE %s STATS_PERSISTENT = 1, STATS_AUTO_RECALC = 0, STATS_SAMPLE_PAGES = %d"
                        .formatted(quoteIdentifier(tableName), Constants.MYSQL_INNODB_STATS_PERSISTENT_SAMPLE_PAGES))
                .toList();
    }

    @Override
    public String analyzeTablesSql(List<String> tableNames) {
        if (tableNames.isEmpty()) {
            return "";
        }

        return "ANALYZE TABLE " + tableNames.stream()
                .map(this::quoteIdentifier)
                .collect(Collectors.joining(", "));
    }

    @Override
    public String explainSql(String sql) {
        return "EXPLAIN FORMAT=JSON " + sql;
    }

    @Override
    public String planSql(String sql, ExecutionMode mode) {
        // EXPLAIN ANALYZE 등 직접 실행할 실행계획 SQL 유지
        if (mode != ExecutionMode.EXPLAIN) {
            return sql;
        }

        // MySQL 실행계획 비용 추출용 JSON EXPLAIN 여부 확인
        String normalizedSql = sql.trim();
        String upperSql = normalizedSql.toUpperCase(Locale.ROOT);
        if (upperSql.startsWith("EXPLAIN FORMAT=JSON ")) {
            return normalizedSql;
        }

        // 일반 EXPLAIN 문장을 JSON EXPLAIN 문장으로 변환
        if (upperSql.startsWith("EXPLAIN ")) {
            return "EXPLAIN FORMAT=JSON " + normalizedSql.substring("EXPLAIN ".length()).trim();
        }

        // SELECT 문장 기준 JSON EXPLAIN 문장 생성
        return explainSql(normalizedSql);
    }

    @Override
    public String selectCountSql(String sql) {
        return "SELECT COUNT(*) FROM (" + sql + ") result_count";
    }

    @Override
    public String selectPageSql(String sql) {
        return "SELECT * FROM (" + sql + ") result_page LIMIT ? OFFSET ?";
    }

    private String quoteLiteral(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
