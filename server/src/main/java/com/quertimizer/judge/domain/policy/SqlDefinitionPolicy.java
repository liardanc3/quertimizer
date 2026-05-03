package com.quertimizer.judge.domain.policy;

import com.quertimizer.judge.application.service.SqlStatementParser;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SqlDefinitionPolicy {

    private final SqlStatementParser statementParser;

    public SqlDefinitionPolicy(SqlStatementParser statementParser) {
        this.statementParser = Objects.requireNonNull(statementParser, "SQL 문장 파서가 필요합니다.");
    }

    public void validateDdl(String ddl) {
        validateRequiredText(ddl, "DDL이 필요합니다.");
        List<String> statements = statementParser.splitStatements(ddl);
        if (statements.isEmpty()) {
            throw new IllegalArgumentException("DDL이 필요합니다.");
        }

        for (String statement : statements) {
            String normalized = normalize(statement);
            validateDangerousKeyword(normalized);

            if (normalized.startsWith("CREATE TABLE ")
                    || normalized.startsWith("ALTER TABLE ")
                    || normalized.startsWith("COMMENT ON TABLE ")
                    || normalized.startsWith("COMMENT ON COLUMN ")
                    || normalized.startsWith("CREATE INDEX ")
                    || normalized.startsWith("CREATE UNIQUE INDEX ")) {
                continue;
            }

            throw new IllegalArgumentException("데이터셋 DDL에는 테이블, 코멘트, 인덱스 문장만 사용할 수 있습니다.");
        }
    }

    public void validateDataSql(String dataSql) {
        validateInsertOnly(dataSql, "데이터 SQL이 필요합니다.");
    }

    public void validateSetupSqls(List<String> setupSqls) {
        Objects.requireNonNull(setupSqls, "설정 SQL 목록이 필요합니다.");

        for (String setupSql : setupSqls) {
            validateRequiredText(setupSql, "설정 SQL은 비어 있을 수 없습니다.");
            for (String statement : statementParser.splitStatements(setupSql)) {
                String normalized = normalize(statement);
                validateDangerousKeyword(normalized);
                if (normalized.startsWith("CREATE INDEX ")
                        || normalized.startsWith("CREATE UNIQUE INDEX ")
                        || normalized.startsWith("ALTER TABLE ")) {
                    continue;
                }

                throw new IllegalArgumentException("설정 SQL에는 인덱스 또는 테이블 변경 문장만 사용할 수 있습니다.");
            }
        }
    }

    public void validateReadOnlySql(String sql) {
        validateRequiredText(sql, "SQL이 필요합니다.");
        List<String> statements = statementParser.splitStatements(sql);
        if (statements.size() != 1) {
            throw new IllegalArgumentException("읽기 전용 SQL 문장은 하나만 허용됩니다.");
        }

        String normalized = normalize(statements.get(0));
        validateDangerousKeyword(normalized);
        if (normalized.startsWith("SELECT ") || normalized.startsWith("WITH ")) {
            return;
        }

        throw new IllegalArgumentException("SELECT 또는 읽기 전용 WITH SQL만 허용됩니다.");
    }

    private void validateInsertOnly(String sql, String requiredMessage) {
        // 데이터 SQL 필수 입력과 문장 존재 여부 검증
        validateRequiredText(sql, requiredMessage);
        List<String> statements = statementParser.splitStatements(sql);
        if (statements.isEmpty()) {
            throw new IllegalArgumentException(requiredMessage);
        }

        // INSERT 문장만 포함되었는지 검증
        for (String statement : statements) {
            String normalized = normalize(statement);
            validateDangerousKeyword(normalized);
            if (!normalized.startsWith("INSERT INTO ")) {
                throw new IllegalArgumentException("데이터셋 데이터 SQL에는 INSERT 문장만 사용할 수 있습니다.");
            }
        }
    }

    private void validateDangerousKeyword(String normalizedSql) {
        // 정의 SQL에서 허용하지 않는 위험 키워드 포함 여부 검증
        if (normalizedSql.contains("DROP TABLE")
                || normalizedSql.contains("DROP SCHEMA")
                || normalizedSql.contains("TRUNCATE")
                || normalizedSql.contains("VACUUM")
                || normalizedSql.contains("REINDEX")
                || normalizedSql.contains("COPY")
                || normalizedSql.contains("PROGRAM")
                || normalizedSql.contains("CREATE EXTENSION")
                || normalizedSql.contains("ALTER SYSTEM")
                || normalizedSql.contains("PG_CATALOG")
                || normalizedSql.contains("INFORMATION_SCHEMA")) {
            throw new IllegalArgumentException("SQL에 허용되지 않는 문장이 포함되어 있습니다.");
        }
    }

    private void validateRequiredText(String value, String message) {
        // 필수 문자열 존재 여부 검증
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private String normalize(String sql) {
        // SQL 비교용 대문자 정규화
        return sql.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
