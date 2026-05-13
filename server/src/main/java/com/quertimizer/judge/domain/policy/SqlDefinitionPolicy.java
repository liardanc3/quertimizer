package com.quertimizer.judge.domain.policy;

import com.quertimizer.judge.domain.service.SqlStatementParser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

import static com.quertimizer.judge.domain.model.SqlPolicyFailReason.DANGEROUS_SQL_INCLUDED;
import static com.quertimizer.judge.domain.model.SqlPolicyFailReason.DATASET_DATA_SQL_COMMAND_UNSUPPORTED;
import static com.quertimizer.judge.domain.model.SqlPolicyFailReason.DATASET_DDL_COMMAND_UNSUPPORTED;
import static com.quertimizer.judge.domain.model.SqlPolicyFailReason.DATA_SQL_REQUIRED;
import static com.quertimizer.judge.domain.model.SqlPolicyFailReason.DDL_REQUIRED;
import static com.quertimizer.judge.domain.model.SqlPolicyFailReason.READ_ONLY_SINGLE_SQL_ONLY;
import static com.quertimizer.judge.domain.model.SqlPolicyFailReason.READ_ONLY_SQL_REQUIRED;
import static com.quertimizer.judge.domain.model.SqlPolicyFailReason.SETUP_SQL_COMMAND_UNSUPPORTED;
import static com.quertimizer.judge.domain.model.SqlPolicyFailReason.SETUP_SQL_REQUIRED;
import static com.quertimizer.judge.domain.model.SqlPolicyFailReason.SQL_REQUIRED;

@Component
@RequiredArgsConstructor
public class SqlDefinitionPolicy {

    private final SqlStatementParser statementParser;

    public void validateDdl(String ddl) {
        validateRequiredText(ddl, DDL_REQUIRED.getMessage());
        List<String> statements = statementParser.splitStatements(ddl);
        if (statements.isEmpty()) {
            throw new IllegalArgumentException(DDL_REQUIRED.getMessage());
        }

        for (String statement : statements) {
            String normalized = normalize(statement);
            validateDangerousKeyword(normalized);

            if (normalized.startsWith("CREATE TABLE ")
                    || normalized.startsWith("ALTER TABLE ")
                    || normalized.startsWith("COMMENT ON TABLE ")
                    || normalized.startsWith("COMMENT ON COLUMN ")
                    || normalized.startsWith("CREATE INDEX ")
                    || normalized.startsWith("CREATE UNIQUE INDEX ")
                    || normalized.startsWith("CREATE FULLTEXT INDEX ")
                    || normalized.startsWith("CREATE SPATIAL INDEX ")) {
                continue;
            }

            throw new IllegalArgumentException(DATASET_DDL_COMMAND_UNSUPPORTED.getMessage());
        }
    }

    public void validateDataSql(String dataSql) {
        validateInsertOnly(dataSql, DATA_SQL_REQUIRED.getMessage());
    }

    public void validateSetupSqls(List<String> setupSqls) {
        for (String setupSql : setupSqls) {
            validateRequiredText(setupSql, SETUP_SQL_REQUIRED.getMessage());
            for (String statement : statementParser.splitStatements(setupSql)) {
                String normalized = normalize(statement);
                validateDangerousKeyword(normalized);
                if (normalized.startsWith("CREATE INDEX ")
                        || normalized.startsWith("CREATE UNIQUE INDEX ")
                        || normalized.startsWith("CREATE FULLTEXT INDEX ")
                        || normalized.startsWith("CREATE SPATIAL INDEX ")
                        || normalized.startsWith("ALTER TABLE ")) {
                    continue;
                }

                throw new IllegalArgumentException(SETUP_SQL_COMMAND_UNSUPPORTED.getMessage());
            }
        }
    }

    public void validateReadOnlySql(String sql) {
        validateRequiredText(sql, SQL_REQUIRED.getMessage());
        List<String> statements = statementParser.splitStatements(sql);
        if (statements.size() != 1) {
            throw new IllegalArgumentException(READ_ONLY_SINGLE_SQL_ONLY.getMessage());
        }

        String normalized = normalize(statements.get(0));
        validateDangerousKeyword(normalized);
        if (normalized.startsWith("SELECT ") || normalized.startsWith("WITH ")) {
            return;
        }

        throw new IllegalArgumentException(READ_ONLY_SQL_REQUIRED.getMessage());
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
                throw new IllegalArgumentException(DATASET_DATA_SQL_COMMAND_UNSUPPORTED.getMessage());
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
            throw new IllegalArgumentException(DANGEROUS_SQL_INCLUDED.getMessage());
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
