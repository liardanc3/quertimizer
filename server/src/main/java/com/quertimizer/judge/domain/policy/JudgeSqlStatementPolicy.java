package com.quertimizer.judge.domain.policy;

import com.quertimizer.judge.domain.model.JudgeSqlExecutionMode;
import com.quertimizer.judge.domain.model.JudgeSqlStatementPattern;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Matcher;

import static com.quertimizer.problem.domain.model.ProblemQueryFailReason.*;

@Component
public class JudgeSqlStatementPolicy {

    private static final int MAX_SQL_LENGTH = 20_000;

    public void validateInteractiveSql(String sql, String handle) {
        // 인터랙티브 실행 SQL은 단일 statement만 허용
        validateSqlText(sql);
        if (JudgeSqlStatementPattern.MULTI_STATEMENT.findIn(sql)) {
            throw new IllegalArgumentException(SINGLE_SQL_ONLY.getMessage());
        }

        validateSqlStatement(sql, handle);
    }

    public void validateSqlStatement(String sql, String handle) {
        // 실행 가능한 SQL 구문 범위를 검증
        validateSqlText(sql);
        String normalizedSql = normalizeSql(sql);
        validateForbiddenKeyword(normalizedSql, "ALTER SYSTEM", ALTER_SYSTEM_UNAVAILABLE.getMessage());
        validateForbiddenKeyword(normalizedSql, "COPY", COPY_UNAVAILABLE.getMessage());
        validateForbiddenKeyword(normalizedSql, "PROGRAM", PROGRAM_UNAVAILABLE.getMessage());
        validateForbiddenKeyword(normalizedSql, "CREATE EXTENSION", CREATE_EXTENSION_UNAVAILABLE.getMessage());
        validateForbiddenKeyword(normalizedSql, "DROP SCHEMA", DROP_SCHEMA_UNAVAILABLE.getMessage());
        validateForbiddenKeyword(normalizedSql, "DROP TABLE", DROP_TABLE_UNAVAILABLE.getMessage());
        validateForbiddenKeyword(normalizedSql, "TRUNCATE", TRUNCATE_UNAVAILABLE.getMessage());
        validateForbiddenKeyword(normalizedSql, "VACUUM", VACUUM_UNAVAILABLE.getMessage());
        validateForbiddenKeyword(normalizedSql, "REINDEX", REINDEX_UNAVAILABLE.getMessage());
        validateForbiddenKeyword(normalizedSql, "PG_CATALOG", PG_CATALOG_ACCESS_DENIED.getMessage());
        validateForbiddenKeyword(normalizedSql, "INFORMATION_SCHEMA", INFORMATION_SCHEMA_ACCESS_DENIED.getMessage());
        validateForbiddenKeyword(normalizedSql, "CONCURRENTLY", CONCURRENTLY_UNAVAILABLE.getMessage());

        if (JudgeSqlStatementPattern.TEMPLATE.findIn(sql)) {
            throw new IllegalArgumentException(TEMPLATE_TABLE_ACCESS_DENIED.getMessage());
        }

        if (JudgeSqlStatementPattern.BASE_WORKSPACE.findIn(sql)) {
            throw new IllegalArgumentException(BASE_WORKSPACE_ACCESS_DENIED.getMessage());
        }

        if (JudgeSqlStatementPattern.SESSION_SCHEMA.findIn(sql)) {
            throw new IllegalArgumentException(OTHER_SESSION_SCHEMA_ACCESS_DENIED.getMessage());
        }

        Matcher otherWorkspaceMatcher = JudgeSqlStatementPattern.OTHER_WORKSPACE.matcher(sql);
        String currentWorkspacePrefix = sanitizeWorkspacePrefix(handle);
        while (otherWorkspaceMatcher.find()) {
            String schemaName = otherWorkspaceMatcher.group().toLowerCase(Locale.ROOT);
            if (!schemaName.startsWith(currentWorkspacePrefix + "_problem_")) {
                throw new IllegalArgumentException(OTHER_USER_WORKSPACE_ACCESS_DENIED.getMessage());
            }
        }
    }

    public void validateSqlText(String sql) {
        // SQL 텍스트 길이와 필수값을 검증
        if (sql.isBlank()) {
            throw new IllegalArgumentException(SQL_REQUIRED.getMessage());
        }

        if (sql.length() > MAX_SQL_LENGTH) {
            throw new IllegalArgumentException(SQL_LENGTH_EXCEEDED.getMessage());
        }
    }

    public JudgeSqlExecutionMode resolveExecutionMode(String sql) {
        // SQL 구문 기준 실행 모드 결정
        String normalizedSql = normalizeSql(sql);

        if (JudgeSqlStatementPattern.EXPLAIN_ANALYZE.findIn(normalizedSql)) {
            return JudgeSqlExecutionMode.EXPLAIN_ANALYZE;
        }

        if (normalizedSql.startsWith("EXPLAIN ")) {
            return JudgeSqlExecutionMode.EXPLAIN;
        }

        if (JudgeSqlStatementPattern.CREATE_INDEX.findIn(normalizedSql)
                || JudgeSqlStatementPattern.DROP_INDEX.findIn(normalizedSql)
                || JudgeSqlStatementPattern.ALTER_INDEX.findIn(normalizedSql)) {
            return JudgeSqlExecutionMode.INDEX_COMMAND;
        }

        if (normalizedSql.startsWith("SELECT ")) {
            return JudgeSqlExecutionMode.SELECT;
        }

        if (normalizedSql.startsWith("WITH ")) {
            if (JudgeSqlStatementPattern.WRITE_CTE.findIn(normalizedSql)) {
                throw new IllegalArgumentException(WRITE_CTE_UNSUPPORTED.getMessage());
            }

            return JudgeSqlExecutionMode.SELECT;
        }

        throw new IllegalArgumentException(UNSUPPORTED_SQL_COMMAND.getMessage());
    }

    public JudgeSqlExecutionMode resolveSubmitExecutionMode(String sql) {
        // 제출 SQL에서 허용되는 실행 모드만 결정
        JudgeSqlExecutionMode executionMode = resolveExecutionMode(sql);
        if (executionMode == JudgeSqlExecutionMode.EXPLAIN || executionMode == JudgeSqlExecutionMode.EXPLAIN_ANALYZE) {
            throw new IllegalArgumentException(SUBMIT_SELECT_AND_INDEX_DDL_ONLY.getMessage());
        }

        return executionMode;
    }

    private void validateForbiddenKeyword(String normalizedSql, String keyword, String message) {
        // 금지 키워드 포함 여부를 검증
        if (normalizedSql.contains(keyword)) {
            throw new IllegalArgumentException(message);
        }
    }

    private String normalizeSql(String sql) {
        // SQL 비교용 문자열 정규화
        return sql.trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private String sanitizeWorkspacePrefix(String handle) {
        // 사용자 Handle 기반 작업 스키마 prefix 정규화
        String sanitizedHandle = handle.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        if (sanitizedHandle.isBlank()) {
            sanitizedHandle = "user";
        }

        if (Character.isDigit(sanitizedHandle.charAt(0))) {
            sanitizedHandle = "u_" + sanitizedHandle;
        }

        int maxPrefixLength = Math.max(1, 63 - "_problem_00001_00001".length());
        return sanitizedHandle.length() > maxPrefixLength
                ? sanitizedHandle.substring(0, maxPrefixLength)
                : sanitizedHandle;
    }
}
