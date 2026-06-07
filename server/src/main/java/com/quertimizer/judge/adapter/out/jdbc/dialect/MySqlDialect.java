package com.quertimizer.judge.adapter.out.jdbc.dialect;

import com.quertimizer.judge.application.port.out.SqlDialect;
import com.quertimizer.judge.config.JudgeStatisticsProperties;
import com.quertimizer.judge.domain.model.ExecutionMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class MySqlDialect implements SqlDialect {

    private final JudgeStatisticsProperties statisticsProperties;

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
                        .formatted(quoteIdentifier(tableName), statisticsProperties.getMysql().getInnodbStatsPersistentSamplePages()))
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
        // MySQL 실행 계획은 JSON 형식으로 변환
        if (mode != ExecutionMode.EXPLAIN) {
            return sql;
        }

        // 이미 MySQL FORMAT 옵션을 포함한 실행 계획 SQL은 그대로 사용
        String trimmedSql = sql.trim();
        if (trimmedSql.regionMatches(true, 0, "EXPLAIN FORMAT=", 0, "EXPLAIN FORMAT=".length())) {
            return trimmedSql;
        }

        // 일반 EXPLAIN SQL은 비용 추출 가능한 JSON 형식으로 변환
        if (trimmedSql.regionMatches(true, 0, "EXPLAIN", 0, "EXPLAIN".length())) {
            return explainSql(trimmedSql.substring("EXPLAIN".length()).trim());
        }

        return explainSql(trimmedSql);
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
