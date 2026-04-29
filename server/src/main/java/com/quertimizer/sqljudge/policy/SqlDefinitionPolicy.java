package com.quertimizer.sqljudge.policy;

import com.quertimizer.sqljudge.runtime.SqlStatementParser;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * Validates SQL definition material accepted by sql-judge.
 */
public class SqlDefinitionPolicy {

    private final SqlStatementParser statementParser;

    /**
     * Creates a SQL definition validation policy.
     *
     * @param statementParser SQL statement parser
     */
    public SqlDefinitionPolicy(SqlStatementParser statementParser) {
        this.statementParser = Objects.requireNonNull(statementParser, "statementParser must not be null");
    }

    /**
     * Validates schema DDL for a registered dataset.
     *
     * @param ddl schema DDL
     */
    public void validateDdl(String ddl) {
        validateRequiredText(ddl, "DDL is required");
        List<String> statements = statementParser.splitStatements(ddl);
        if (statements.isEmpty()) {
            throw new IllegalArgumentException("DDL is required");
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

            throw new IllegalArgumentException("Dataset DDL can only contain table, comment, and index statements");
        }
    }

    /**
     * Validates data SQL for a registered dataset.
     *
     * @param dataSql data SQL
     */
    public void validateDataSql(String dataSql) {
        validateInsertOnly(dataSql, "Data SQL is required");
    }

    /**
     * Validates setup SQL for a registered setup SQL bundle.
     *
     * @param setupSqls setup SQL statements
     */
    public void validateSetupSqls(List<String> setupSqls) {
        Objects.requireNonNull(setupSqls, "setupSqls must not be null");

        for (String setupSql : setupSqls) {
            validateRequiredText(setupSql, "Setup SQL must not be blank");
            for (String statement : statementParser.splitStatements(setupSql)) {
                String normalized = normalize(statement);
                validateDangerousKeyword(normalized);
                if (normalized.startsWith("CREATE INDEX ")
                        || normalized.startsWith("CREATE UNIQUE INDEX ")
                        || normalized.startsWith("ALTER TABLE ")) {
                    continue;
                }

                throw new IllegalArgumentException("Setup SQL can only contain index or table alteration statements");
            }
        }
    }

    /**
     * Validates read-only SQL for reference and target execution.
     *
     * @param sql SQL statement
     */
    public void validateReadOnlySql(String sql) {
        validateRequiredText(sql, "SQL is required");
        List<String> statements = statementParser.splitStatements(sql);
        if (statements.size() != 1) {
            throw new IllegalArgumentException("Only one read-only SQL statement is allowed");
        }

        String normalized = normalize(statements.get(0));
        validateDangerousKeyword(normalized);
        if (normalized.startsWith("SELECT ") || normalized.startsWith("WITH ")) {
            return;
        }

        throw new IllegalArgumentException("Only SELECT or read-only WITH SQL is allowed");
    }

    private void validateInsertOnly(String sql, String requiredMessage) {
        validateRequiredText(sql, requiredMessage);
        List<String> statements = statementParser.splitStatements(sql);
        if (statements.isEmpty()) {
            throw new IllegalArgumentException(requiredMessage);
        }

        for (String statement : statements) {
            String normalized = normalize(statement);
            validateDangerousKeyword(normalized);
            if (!normalized.startsWith("INSERT INTO ")) {
                throw new IllegalArgumentException("Dataset data SQL can only contain INSERT statements");
            }
        }
    }

    private void validateDangerousKeyword(String normalizedSql) {
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
            throw new IllegalArgumentException("SQL contains a disallowed statement");
        }
    }

    private void validateRequiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    private String normalize(String sql) {
        return sql.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
    }
}
