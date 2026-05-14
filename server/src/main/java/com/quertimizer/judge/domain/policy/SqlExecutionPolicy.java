package com.quertimizer.judge.domain.policy;

import com.quertimizer.judge.domain.model.ExecutionMode;
import com.quertimizer.judge.domain.service.SqlStatementParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

import static com.quertimizer.judge.domain.model.SqlPolicyFailReason.DANGEROUS_SQL_INCLUDED;
import static com.quertimizer.judge.domain.model.SqlPolicyFailReason.SINGLE_SQL_ONLY;
import static com.quertimizer.judge.domain.model.SqlPolicyFailReason.SQL_LENGTH_EXCEEDED;
import static com.quertimizer.judge.domain.model.SqlPolicyFailReason.SQL_REQUIRED;
import static com.quertimizer.judge.domain.model.SqlPolicyFailReason.UNSUPPORTED_SQL_COMMAND;
import static com.quertimizer.judge.domain.model.SqlPolicyFailReason.WRITE_CTE_UNSUPPORTED;

@Component
@RequiredArgsConstructor
public class SqlExecutionPolicy {

    private final SqlStatementParser statementParser;

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
                throw new IllegalArgumentException(WRITE_CTE_UNSUPPORTED.getMessage());
            }

            return ExecutionMode.SELECT;
        }

        if (normalizedSql.startsWith("CREATE INDEX ")
                || normalizedSql.startsWith("CREATE UNIQUE INDEX ")
                || normalizedSql.startsWith("CREATE FULLTEXT INDEX ")
                || normalizedSql.startsWith("CREATE SPATIAL INDEX ")
                || normalizedSql.startsWith("DROP INDEX ")
                || normalizedSql.startsWith("ALTER INDEX ")) {
            return ExecutionMode.INDEX_COMMAND;
        }

        throw new IllegalArgumentException(UNSUPPORTED_SQL_COMMAND.getMessage());
    }

    private String validateSingleStatement(String sql) {
        // SQL 존재 여부와 길이 검증
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException(SQL_REQUIRED.getMessage());
        }

        if (sql.length() > SqlExecutionRules.MAX_SQL_LENGTH) {
            throw new IllegalArgumentException(SQL_LENGTH_EXCEEDED.getMessage());
        }

        // 단일 문장 여부 검증
        List<String> statements = statementParser.splitStatements(sql);
        if (statements.size() != 1) {
            throw new IllegalArgumentException(SINGLE_SQL_ONLY.getMessage());
        }

        // 위험 키워드 검증 후 정규화 SQL 반환
        String normalizedSql = normalize(statements.get(0));
        validateDangerousKeyword(normalizedSql);
        return removeLeadingComments(normalizedSql);
    }

    private void validateDangerousKeyword(String normalizedSql) {
        // 실행 금지 키워드 포함 여부 검증
        if (SqlExecutionRules.containsDangerousKeyword(normalizedSql)) {
            throw new IllegalArgumentException(DANGEROUS_SQL_INCLUDED.getMessage());
        }
    }

    private String normalize(String sql) {
        // SQL 비교용 대문자 정규화
        return sql.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }

    private String removeLeadingComments(String normalizedSql) {
        // 선행 힌트 주석 제거 후 실행 SQL 유형 판별 기준 반환
        String resolvedSql = normalizedSql;
        boolean changed = true;
        while (changed) {
            changed = false;
            if (resolvedSql.startsWith("/*")) {
                int endIndex = resolvedSql.indexOf("*/");
                if (endIndex >= 0) {
                    resolvedSql = resolvedSql.substring(endIndex + 2).trim();
                    changed = true;
                }
            }
        }

        return resolvedSql;
    }
}
