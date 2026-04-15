package com.quertimizer.service;

import com.quertimizer.constant.DbmsType;
import com.quertimizer.entity.ProblemSubmitHistory;
import com.quertimizer.repository.ProblemSubmitHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProblemQueryService {

    private static final int QUERY_TIMEOUT_SECONDS = 60;
    private static final int MAX_SQL_LENGTH = 20_000;
    private static final Pattern CREATE_INDEX_PATTERN = Pattern.compile("^CREATE\\s+(UNIQUE\\s+)?INDEX\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DROP_INDEX_PATTERN = Pattern.compile("^DROP\\s+INDEX\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPLAIN_ANALYZE_PATTERN = Pattern.compile("^EXPLAIN\\s+(\\([^)]*ANALYZE[^)]*\\)|ANALYZE\\b)", Pattern.CASE_INSENSITIVE);
    private static final Pattern OTHER_WORKSPACE_PATTERN = Pattern.compile("\\b[a-z0-9_]+_problem_\\d{5}_\\d{5}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern BASE_WORKSPACE_PATTERN = Pattern.compile("\\bproblem_set_\\d{5}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEMPLATE_PATTERN = Pattern.compile("\\bproblem_[a-z0-9_]+_template\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MULTI_STATEMENT_PATTERN = Pattern.compile(";(?=.+\\S)");
    private static final Pattern WRITE_CTE_PATTERN = Pattern.compile("\\bWITH\\b[\\s\\S]*\\b(INSERT|UPDATE|DELETE|MERGE)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAN_TOTAL_COST_PATTERN = Pattern.compile("cost=[0-9]+(?:\\.[0-9]+)?\\.\\.([0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);

    private final DataSource dataSource;
    private final ProblemWorkspaceService problemWorkspaceService;
    private final ProblemSubmitHistoryRepository problemSubmitHistoryRepository;

    public QueryExecutionResult executeInteractiveSql(String userId,
                                                      String socketId,
                                                      String problemId,
                                                      String sql,
                                                      DbmsType dbmsType) {
        if (dbmsType != DbmsType.POSTGRESQL) {
            throw new IllegalArgumentException("인터랙티브 실행은 PostgreSQL만 지원한다.");
        }

        String trimmedSql = trimTrailingSemicolon(sql);
        validateSql(trimmedSql, userId);
        ExecutionMode executionMode = resolveExecutionMode(trimmedSql);
        ProblemWorkspaceService.WorkspaceHandle workspaceHandle =
                problemWorkspaceService.prepareWorkspace(userId, problemId, socketId);

        try (Connection connection = dataSource.getConnection()) {

            // 작업용 스키마 우선 사용
            connection.setAutoCommit(false);
            configureExecutionConnection(connection, workspaceHandle.schemaName());

            Double estimatedCost = executionMode == ExecutionMode.SELECT
                    ? estimateQueryCost(connection, trimmedSql)
                    : null;
            long startTime = System.nanoTime();
            QueryExecutionResult executionResult = switch (executionMode) {
                case SELECT -> executeSelect(connection, trimmedSql, problemId, estimatedCost);
                case EXPLAIN -> executeExplain(connection, trimmedSql, problemId, "explain");
                case EXPLAIN_ANALYZE -> executeExplain(connection, trimmedSql, problemId, "explain_analyze");
                case INDEX_COMMAND -> executeIndexCommand(connection, trimmedSql, problemId);
            };

            connection.commit();
            problemWorkspaceService.markActivity(socketId);

            return executionResult.withExecutionTimeMs(Duration.ofNanos(System.nanoTime() - startTime).toMillis());
        } catch (SQLException exception) {
            throw new IllegalStateException(resolveSqlErrorMessage(exception), exception);
        }
    }

    @Transactional
    public ProblemSubmitResult submitProblemSql(String userId,
                                               String socketId,
                                               String problemId,
                                               String sql,
                                               DbmsType dbmsType) {
        LocalDateTime submittedAt = LocalDateTime.now();
        String submittedProblemId = normalizeProblemId(problemId);
        String submittedSql = normalizeSubmittedSql(sql);

        try {
            QueryExecutionResult executionResult = executeSubmittedSql(userId, socketId, submittedProblemId, submittedSql, dbmsType);

            problemSubmitHistoryRepository.save(ProblemSubmitHistory.create(
                    submittedProblemId,
                    userId,
                    dbmsType,
                    submittedSql,
                    true,
                    "제출을 기록했다.",
                    executionResult.executionTimeMs(),
                    executionResult.cost() != null ? executionResult.cost() : 0d,
                    executionResult.rowCount(),
                    submittedAt
            ));

            return ProblemSubmitResult.success(submittedProblemId, "제출을 기록했다.", executionResult.executionTimeMs());
        } catch (Exception exception) {
            String message = resolveProblemSubmitErrorMessage(exception);

            problemSubmitHistoryRepository.save(ProblemSubmitHistory.create(
                    submittedProblemId,
                    userId,
                    dbmsType,
                    submittedSql,
                    false,
                    message,
                    0,
                    0d,
                    0,
                    submittedAt
            ));

            return ProblemSubmitResult.failure(submittedProblemId, message);
        }
    }

    private void validateSql(String sql, String userId) {
        if (sql.isBlank()) {
            throw new IllegalArgumentException("실행할 SQL을 입력해라.");
        }

        if (sql.length() > MAX_SQL_LENGTH) {
            throw new IllegalArgumentException("SQL 길이 제한을 초과했다.");
        }

        if (MULTI_STATEMENT_PATTERN.matcher(sql).find()) {
            throw new IllegalArgumentException("한 번에 하나의 SQL만 실행할 수 있다.");
        }

        String normalizedSql = normalizeSql(sql);
        validateForbiddenKeyword(normalizedSql, "ALTER SYSTEM", "ALTER SYSTEM은 사용할 수 없다.");
        validateForbiddenKeyword(normalizedSql, "COPY", "COPY는 사용할 수 없다.");
        validateForbiddenKeyword(normalizedSql, "PROGRAM", "PROGRAM은 사용할 수 없다.");
        validateForbiddenKeyword(normalizedSql, "CREATE EXTENSION", "CREATE EXTENSION은 사용할 수 없다.");
        validateForbiddenKeyword(normalizedSql, "DROP SCHEMA", "DROP SCHEMA는 사용할 수 없다.");
        validateForbiddenKeyword(normalizedSql, "DROP TABLE", "DROP TABLE은 사용할 수 없다.");
        validateForbiddenKeyword(normalizedSql, "TRUNCATE", "TRUNCATE는 사용할 수 없다.");
        validateForbiddenKeyword(normalizedSql, "VACUUM", "VACUUM은 사용할 수 없다.");
        validateForbiddenKeyword(normalizedSql, "REINDEX", "REINDEX는 사용할 수 없다.");
        validateForbiddenKeyword(normalizedSql, "PG_CATALOG", "pg_catalog에는 접근할 수 없다.");
        validateForbiddenKeyword(normalizedSql, "INFORMATION_SCHEMA", "information_schema에는 접근할 수 없다.");
        validateForbiddenKeyword(normalizedSql, "CONCURRENTLY", "CONCURRENTLY는 사용할 수 없다.");

        if (TEMPLATE_PATTERN.matcher(sql).find()) {
            throw new IllegalArgumentException("템플릿 테이블에는 직접 접근할 수 없다.");
        }

        if (BASE_WORKSPACE_PATTERN.matcher(sql).find()) {
            throw new IllegalArgumentException("원본 테이블셋에는 직접 접근할 수 없다.");
        }

        if (Pattern.compile("\\bsession_[a-z0-9_]+\\b", Pattern.CASE_INSENSITIVE).matcher(sql).find()) {
            throw new IllegalArgumentException("다른 세션 스키마에는 접근할 수 없다.");
        }

        Matcher otherWorkspaceMatcher = OTHER_WORKSPACE_PATTERN.matcher(sql);
        String currentWorkspacePrefix = sanitizeWorkspacePrefix(userId);
        while (otherWorkspaceMatcher.find()) {
            String schemaName = otherWorkspaceMatcher.group().toLowerCase(Locale.ROOT);
            if (!schemaName.startsWith(currentWorkspacePrefix + "_problem_")) {
                throw new IllegalArgumentException("다른 사용자 작업용 스키마에는 접근할 수 없다.");
            }
        }
    }

    private void configureExecutionConnection(Connection connection, String schemaName) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute("SET LOCAL search_path TO " + quoteIdentifier(schemaName) + ", public");
            statement.execute("SET LOCAL statement_timeout TO '" + QUERY_TIMEOUT_SECONDS + "s'");
        }
    }

    private QueryExecutionResult executeSelect(Connection connection, String sql, String problemId, Double cost) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            statement.execute(sql);

            try (ResultSet resultSet = statement.getResultSet()) {
                if (resultSet == null) {
                    throw new IllegalArgumentException("SELECT 결과를 확인할 수 없다.");
                }

                ResultSetMetaData metaData = resultSet.getMetaData();
                List<String> columns = new ArrayList<>();
                for (int columnIndex = 1; columnIndex <= metaData.getColumnCount(); columnIndex++) {
                    columns.add(metaData.getColumnLabel(columnIndex));
                }

                List<List<String>> rows = new ArrayList<>();
                long rowCount = 0;
                while (resultSet.next()) {
                    rowCount++;

                    List<String> row = new ArrayList<>();
                    for (int columnIndex = 1; columnIndex <= metaData.getColumnCount(); columnIndex++) {
                        row.add(String.valueOf(resultSet.getObject(columnIndex)));
                    }
                    rows.add(row);
                }

                return QueryExecutionResult.selectWithCost(problemId, columns, rows, rowCount, cost);
            }
        }
    }

    private QueryExecutionResult executeExplain(Connection connection, String sql, String problemId, String mode) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            statement.execute(sql);

            try (ResultSet resultSet = statement.getResultSet()) {
                if (resultSet == null) {
                    throw new IllegalArgumentException("실행 계획을 확인할 수 없다.");
                }

                List<String> planLines = new ArrayList<>();
                while (resultSet.next()) {
                    planLines.add(resultSet.getString(1));
                }

                return QueryExecutionResult.planWithCost(problemId, mode, planLines, extractEstimatedCost(planLines));
            }
        }
    }

    private QueryExecutionResult executeIndexCommand(Connection connection, String sql, String problemId) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            statement.execute(sql);

            return QueryExecutionResult.command(problemId, "인덱스 변경을 반영했다.");
        }
    }

    private ExecutionMode resolveExecutionMode(String sql) {
        String normalizedSql = normalizeSql(sql);

        if (EXPLAIN_ANALYZE_PATTERN.matcher(normalizedSql).find()) {
            return ExecutionMode.EXPLAIN_ANALYZE;
        }

        if (normalizedSql.startsWith("EXPLAIN ")) {
            return ExecutionMode.EXPLAIN;
        }

        if (CREATE_INDEX_PATTERN.matcher(normalizedSql).find() || DROP_INDEX_PATTERN.matcher(normalizedSql).find()) {
            return ExecutionMode.INDEX_COMMAND;
        }

        if (normalizedSql.startsWith("SELECT ")) {
            return ExecutionMode.SELECT;
        }

        if (normalizedSql.startsWith("WITH ")) {
            if (WRITE_CTE_PATTERN.matcher(normalizedSql).find()) {
                throw new IllegalArgumentException("데이터 변경 CTE는 사용할 수 없다.");
            }

            return ExecutionMode.SELECT;
        }

        throw new IllegalArgumentException("SELECT, EXPLAIN, EXPLAIN ANALYZE, CREATE INDEX, DROP INDEX만 실행할 수 있다.");
    }

    private QueryExecutionResult executeSubmittedSql(String userId,
                                                     String socketId,
                                                     String problemId,
                                                     String sql,
                                                     DbmsType dbmsType) {
        if (dbmsType != DbmsType.POSTGRESQL) {
            throw new IllegalArgumentException("제출은 PostgreSQL만 지원한다.");
        }

        validateSql(sql, userId);
        validateSubmittedSql(sql);

        ProblemWorkspaceService.WorkspaceHandle workspaceHandle =
                problemWorkspaceService.prepareWorkspace(userId, problemId, socketId);

        try (Connection connection = dataSource.getConnection()) {

            // 제출용 작업 스키마 기준 SQL 실행
            connection.setAutoCommit(false);
            configureExecutionConnection(connection, workspaceHandle.schemaName());

            Double estimatedCost = estimateQueryCost(connection, sql);
            long startTime = System.nanoTime();
            QueryExecutionResult executionResult = executeSelect(connection, sql, problemId, estimatedCost);

            connection.commit();
            problemWorkspaceService.markActivity(socketId);

            return executionResult.withExecutionTimeMs(Duration.ofNanos(System.nanoTime() - startTime).toMillis());
        } catch (SQLException exception) {
            throw new IllegalStateException(resolveSqlErrorMessage(exception), exception);
        }
    }

    private void validateSubmittedSql(String sql) {
        String normalizedSql = normalizeSql(sql);

        if (normalizedSql.startsWith("SELECT ")) {
            return;
        }

        if (normalizedSql.startsWith("WITH ") && !WRITE_CTE_PATTERN.matcher(normalizedSql).find()) {
            return;
        }

        throw new IllegalArgumentException("제출은 SELECT만 지원한다.");
    }

    private void validateForbiddenKeyword(String normalizedSql, String keyword, String message) {
        if (normalizedSql.contains(keyword)) {
            throw new IllegalArgumentException(message);
        }
    }

    private String trimTrailingSemicolon(String sql) {
        return sql.trim().replaceFirst(";\\s*$", "");
    }

    private String normalizeProblemId(String problemId) {
        return problemId != null ? problemId.trim() : "";
    }

    private String normalizeSubmittedSql(String sql) {
        return sql != null ? trimTrailingSemicolon(sql) : "";
    }

    private String normalizeSql(String sql) {
        return sql.trim()
                .replaceAll("\\s+", " ")
                .toUpperCase(Locale.ROOT);
    }

    private String sanitizeWorkspacePrefix(String userId) {
        String sanitizedUserId = userId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        if (sanitizedUserId.isBlank()) {
            sanitizedUserId = "user";
        }

        if (Character.isDigit(sanitizedUserId.charAt(0))) {
            sanitizedUserId = "u_" + sanitizedUserId;
        }

        int maxPrefixLength = Math.max(1, 63 - "_problem_00001_00001".length());
        return sanitizedUserId.length() > maxPrefixLength
                ? sanitizedUserId.substring(0, maxPrefixLength)
                : sanitizedUserId;
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private Double estimateQueryCost(Connection connection, String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            statement.execute("EXPLAIN " + sql);

            try (ResultSet resultSet = statement.getResultSet()) {
                if (resultSet == null) {
                    return null;
                }

                List<String> planLines = new ArrayList<>();
                while (resultSet.next()) {
                    planLines.add(resultSet.getString(1));
                }

                return extractEstimatedCost(planLines);
            }
        }
    }

    private Double extractEstimatedCost(List<String> planLines) {
        for (String planLine : planLines) {
            if (planLine == null || planLine.isBlank()) {
                continue;
            }

            Matcher matcher = PLAN_TOTAL_COST_PATTERN.matcher(planLine);
            if (matcher.find()) {
                try {
                    return Double.parseDouble(matcher.group(1));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }

        return null;
    }

    private String resolveSqlErrorMessage(SQLException exception) {
        return exception.getMessage() != null && !exception.getMessage().isBlank()
                ? exception.getMessage()
                : "SQL 실행에 실패했다.";
    }

    private String resolveProblemSubmitErrorMessage(Exception exception) {
        return exception.getMessage() != null && !exception.getMessage().isBlank()
                ? exception.getMessage()
                : "제출 처리에 실패했다.";
    }

    private enum ExecutionMode {
        SELECT,
        EXPLAIN,
        EXPLAIN_ANALYZE,
        INDEX_COMMAND
    }

    public record QueryExecutionResult(String problemId,
                                       String mode,
                                       String message,
                                       List<String> columns,
                                       List<List<String>> rows,
                                       List<String> planLines,
                                       long rowCount,
                                       long executionTimeMs,
                                       Double cost) {

        private static QueryExecutionResult select(String problemId,
                                                   List<String> columns,
                                                   List<List<String>> rows,
                                                   long rowCount) {
            return new QueryExecutionResult(problemId, "select", "조회 결과를 반환했다.", columns, rows, List.of(), rowCount, 0, null);
        }

        private static QueryExecutionResult plan(String problemId, String mode, List<String> planLines) {
            return new QueryExecutionResult(problemId, mode, "실행 계획을 반환했다.", List.of(), List.of(), planLines, planLines.size(), 0, null);
        }

        private static QueryExecutionResult selectWithCost(String problemId,
                                                           List<String> columns,
                                                           List<List<String>> rows,
                                                           long rowCount,
                                                           Double cost) {
            QueryExecutionResult executionResult = select(problemId, columns, rows, rowCount);
            return new QueryExecutionResult(
                    executionResult.problemId(),
                    executionResult.mode(),
                    executionResult.message(),
                    executionResult.columns(),
                    executionResult.rows(),
                    executionResult.planLines(),
                    executionResult.rowCount(),
                    executionResult.executionTimeMs(),
                    cost
            );
        }

        private static QueryExecutionResult planWithCost(String problemId, String mode, List<String> planLines, Double cost) {
            QueryExecutionResult executionResult = plan(problemId, mode, planLines);
            return new QueryExecutionResult(
                    executionResult.problemId(),
                    executionResult.mode(),
                    executionResult.message(),
                    executionResult.columns(),
                    executionResult.rows(),
                    executionResult.planLines(),
                    executionResult.rowCount(),
                    executionResult.executionTimeMs(),
                    cost
            );
        }

        private static QueryExecutionResult command(String problemId, String message) {
            return new QueryExecutionResult(problemId, "command", message, List.of(), List.of(), List.of(), 0, 0, null);
        }

        private QueryExecutionResult withExecutionTimeMs(long executionTimeMs) {
            return new QueryExecutionResult(problemId, mode, message, columns, rows, planLines, rowCount, executionTimeMs, cost);
        }

    }

    public record ProblemSubmitResult(String problemId,
                                      boolean success,
                                      String message,
                                      Long executionTimeMs) {

        private static ProblemSubmitResult success(String problemId, String message, long executionTimeMs) {
            return new ProblemSubmitResult(problemId, true, message, executionTimeMs);
        }

        private static ProblemSubmitResult failure(String problemId, String message) {
            return new ProblemSubmitResult(problemId, false, message, null);
        }
    }
}
