package com.quertimizer.judge.domain.policy;

import java.util.List;
import java.util.regex.Pattern;

final class SqlExecutionRules {

    static final int MAX_SQL_LENGTH = 20_000;

    private static final Pattern EXPLAIN_ANALYZE =
            Pattern.compile("^EXPLAIN\\s+(\\([^)]*ANALYZE[^)]*\\)|ANALYZE\\b)", Pattern.CASE_INSENSITIVE);
    private static final Pattern WRITE_CTE =
            Pattern.compile("\\bWITH\\b[\\s\\S]*\\b(INSERT|UPDATE|DELETE|MERGE)\\b", Pattern.CASE_INSENSITIVE);
    private static final List<String> DANGEROUS_KEYWORDS = List.of(
            "ALTER SYSTEM",
            "COPY",
            "PROGRAM",
            "CREATE EXTENSION",
            "DROP SCHEMA",
            "DROP TABLE",
            "TRUNCATE",
            "VACUUM",
            "REINDEX",
            "CONCURRENTLY",
            "PG_CATALOG",
            "INFORMATION_SCHEMA"
    );

    private SqlExecutionRules() {
    }

    static boolean isExplainAnalyze(String normalizedSql) {
        // EXPLAIN ANALYZE 문장 여부 확인
        return EXPLAIN_ANALYZE.matcher(normalizedSql).find();
    }

    static boolean containsWritableCte(String normalizedSql) {
        // 쓰기 CTE 포함 여부 확인
        return WRITE_CTE.matcher(normalizedSql).find();
    }

    static boolean containsDangerousKeyword(String normalizedSql) {
        // 실행 금지 키워드 포함 여부 확인
        for (String dangerousKeyword : DANGEROUS_KEYWORDS) {
            if (normalizedSql.contains(dangerousKeyword)) {
                return true;
            }
        }

        return false;
    }
}
