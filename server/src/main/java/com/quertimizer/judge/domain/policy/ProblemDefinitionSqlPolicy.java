package com.quertimizer.judge.domain.policy;

import com.quertimizer.judge.domain.service.JudgeSqlStatementParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ProblemDefinitionSqlPolicy {

    private final JudgeSqlStatementParser judgeSqlStatementParser;

    public void validateDdl(String ddl) {
        // 문제 정의용 DDL은 CREATE/ALTER TABLE, COMMENT, INDEX 범위만 허용
        validateRequiredText(ddl, "DDL이 필요하다.");
        List<String> statements = judgeSqlStatementParser.splitStatements(ddl);
        if (statements.isEmpty()) {
            throw new IllegalArgumentException("DDL이 필요하다.");
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

            throw new IllegalArgumentException("문제 정의 DDL에는 CREATE/ALTER TABLE, COMMENT, INDEX만 사용할 수 있다.");
        }
    }

    public void validateActualDataSql(String actualDataSql) {
        // 실제 채점 데이터 SQL은 INSERT만 허용
        validateInsertOnly(actualDataSql, "실제 채점 데이터 SQL이 필요하다.");
    }

    public void validateSampleDataSql(String sampleDataSql) {
        // 예시 데이터 SQL은 INSERT만 허용
        validateInsertOnly(sampleDataSql, "예시 데이터 SQL이 필요하다.");
    }

    public void validateAnswerSql(String answerSql) {
        // 기준 정답 SQL은 읽기 전용 SELECT/WITH만 허용
        validateRequiredText(answerSql, "정답 SQL이 필요하다.");
        List<String> statements = judgeSqlStatementParser.splitStatements(answerSql);
        if (statements.size() != 1) {
            throw new IllegalArgumentException("정답 SQL은 하나의 SELECT만 허용한다.");
        }

        String normalized = normalize(statements.get(0));
        validateDangerousKeyword(normalized);
        if (normalized.startsWith("SELECT ") || normalized.startsWith("WITH ")) {
            return;
        }

        throw new IllegalArgumentException("정답 SQL은 SELECT 또는 읽기 전용 WITH만 허용한다.");
    }

    private void validateInsertOnly(String sql, String requiredMessage) {
        validateRequiredText(sql, requiredMessage);
        List<String> statements = judgeSqlStatementParser.splitStatements(sql);
        if (statements.isEmpty()) {
            throw new IllegalArgumentException(requiredMessage);
        }

        for (String statement : statements) {
            String normalized = normalize(statement);
            validateDangerousKeyword(normalized);
            if (!normalized.startsWith("INSERT INTO ")) {
                throw new IllegalArgumentException("문제 생성 데이터 SQL에는 INSERT만 사용할 수 있다.");
            }
        }
    }

    private void validateDangerousKeyword(String normalizedSql) {
        // 운영 DB를 오염시킬 수 있는 위험 SQL을 차단
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
            throw new IllegalArgumentException("문제 생성 SQL에 허용되지 않는 구문이 포함되어 있다.");
        }
    }

    private void validateRequiredText(String value, String message) {
        // 필수 텍스트 존재 여부를 검증
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private String normalize(String sql) {
        return sql.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
