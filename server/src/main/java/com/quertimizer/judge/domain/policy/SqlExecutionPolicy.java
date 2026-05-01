package com.quertimizer.judge.domain.policy;

import com.quertimizer.judge.application.output.ExecutionMode;
import com.quertimizer.judge.infrastructure.runtime.SqlStatementParser;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SqlExecutionPolicy {

    private final SqlStatementParser statementParser;

    public SqlExecutionPolicy(SqlStatementParser statementParser) {
        this.statementParser = Objects.requireNonNull(statementParser, "SQL 문장 파서가 필요합니다.");
    }

    /**
     * 실행 대상 SQL 문장의 실행 모드를 결정한다.
     *
     * <ol>
     *   <li>단일 SQL 문장 검증과 정규화
     *   <li>실행 모드 분류
     * </ol>
     *
     * @param sql 실행 모드를 결정할 SQL 문장
     * @return 실행 모드
     */
    public ExecutionMode resolveMode(String sql) {
        String normalizedSql = validateSingleStatement(sql);

        if (SqlExecutionRules.isExplainAnalyze(normalizedSql)) {
            return ExecutionMode.EXPLAIN_ANALYZE;
        }

        if (normalizedSql.startsWith("EXPLAIN ")) {
            return ExecutionMode.EXPLAIN;
        }

        if (normalizedSql.startsWith("SELECT ")) {
            return ExecutionMode.SELECT;
        }

        if (normalizedSql.startsWith("WITH ")) {
            if (SqlExecutionRules.containsWritableCte(normalizedSql)) {
                throw new IllegalArgumentException("쓰기 CTE 문장은 지원하지 않습니다.");
            }

            return ExecutionMode.SELECT;
        }

        if (normalizedSql.startsWith("CREATE INDEX ")
                || normalizedSql.startsWith("CREATE UNIQUE INDEX ")
                || normalizedSql.startsWith("DROP INDEX ")
                || normalizedSql.startsWith("ALTER INDEX ")) {
            return ExecutionMode.INDEX_COMMAND;
        }

        throw new IllegalArgumentException("지원하지 않는 SQL 명령입니다.");
    }

    private String validateSingleStatement(String sql) {
        // SQL 존재 여부와 길이 검증
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("SQL이 필요합니다.");
        }

        if (sql.length() > SqlExecutionRules.MAX_SQL_LENGTH) {
            throw new IllegalArgumentException("SQL 길이가 제한을 초과했습니다.");
        }

        // 단일 문장 여부 검증
        List<String> statements = statementParser.splitStatements(sql);
        if (statements.size() != 1) {
            throw new IllegalArgumentException("SQL 문장은 하나만 허용됩니다.");
        }

        // 위험 키워드 검증 후 정규화 SQL 반환
        String normalizedSql = normalize(statements.get(0));
        validateDangerousKeyword(normalizedSql);
        return normalizedSql;
    }

    private void validateDangerousKeyword(String normalizedSql) {
        // 실행 금지 키워드 포함 여부 검증
        if (SqlExecutionRules.containsDangerousKeyword(normalizedSql)) {
            throw new IllegalArgumentException("SQL에 허용되지 않는 문장이 포함되어 있습니다.");
        }
    }

    private String normalize(String sql) {
        // SQL 비교용 대문자 정규화
        return sql.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
