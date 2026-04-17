package com.quertimizer.service;

import com.quertimizer.constant.DbmsType;
import com.quertimizer.constant.PostgreSqlExecutionPlanElementIndex;
import com.quertimizer.entity.Problem;
import com.quertimizer.entity.ProblemSolveHistory;
import com.quertimizer.entity.ProblemSolveHistoryId;
import com.quertimizer.entity.ProblemSubmitHistory;
import com.quertimizer.repository.ProblemRepository;
import com.quertimizer.repository.ProblemSolveHistoryRepository;
import com.quertimizer.repository.ProblemSubmitHistoryRepository;
import com.quertimizer.store.ProblemStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class ProblemQueryService {

    private static final int QUERY_TIMEOUT_SECONDS = 60;
    private static final int MAX_SQL_LENGTH = 20_000;
    private static final int CONNECTION_RETRY_COUNT = 3;
    private static final long CONNECTION_RETRY_DELAY_MS = 2_000L;
    private static final int DEFAULT_SELECT_PAGE_SIZE = 10;

    private static final Pattern CREATE_INDEX_PATTERN =
            Pattern.compile("^CREATE\\s+(UNIQUE\\s+)?INDEX\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern DROP_INDEX_PATTERN =
            Pattern.compile("^DROP\\s+INDEX\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXPLAIN_ANALYZE_PATTERN =
            Pattern.compile("^EXPLAIN\\s+(\\([^)]*ANALYZE[^)]*\\)|ANALYZE\\b)", Pattern.CASE_INSENSITIVE);
    private static final Pattern OTHER_WORKSPACE_PATTERN =
            Pattern.compile("\\b[a-z0-9_]+_problem_\\d{5}_\\d{5}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern BASE_WORKSPACE_PATTERN =
            Pattern.compile("\\bproblem_set_\\d{5}\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern TEMPLATE_PATTERN =
            Pattern.compile("\\bproblem_[a-z0-9_]+_template\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern MULTI_STATEMENT_PATTERN = Pattern.compile(";(?=.+\\S)");
    private static final Pattern WRITE_CTE_PATTERN =
            Pattern.compile("\\bWITH\\b[\\s\\S]*\\b(INSERT|UPDATE|DELETE|MERGE)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAN_TOTAL_COST_PATTERN =
            Pattern.compile("cost=[0-9]+(?:\\.[0-9]+)?\\.\\.([0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAN_EXECUTION_TIME_PATTERN =
            Pattern.compile("Execution Time:\\s*([0-9]+(?:\\.[0-9]+)?)\\s*ms", Pattern.CASE_INSENSITIVE);

    private final DataSource dataSource;
    private final ProblemWorkspaceService problemWorkspaceService;
    private final ProblemSubmitHistoryRepository problemSubmitHistoryRepository;
    private final ProblemSolveHistoryRepository problemSolveHistoryRepository;
    private final ProblemRepository problemRepository;
    private final ProblemStore problemStore;
    private final ConcurrentHashMap<String, Statement> activeStatements = new ConcurrentHashMap<>();

    public QueryExecutionResult executeInteractiveSql(String userId,
                                                      String socketId,
                                                      String problemId,
                                                      String sql,
                                                      DbmsType dbmsType,
                                                      Integer page,
                                                      Integer pageSize) {
        if (dbmsType != DbmsType.POSTGRESQL) {
            throw new IllegalArgumentException("인터랙티브 실행은 PostgreSQL만 지원한다.");
        }

        String trimmedSql = trimTrailingSemicolon(sql);
        validateSql(trimmedSql, userId);
        ExecutionMode executionMode = resolveExecutionMode(trimmedSql);
        ProblemWorkspaceService.WorkspaceHandle workspaceHandle =
                problemWorkspaceService.prepareWorkspace(userId, problemId, socketId);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            configureExecutionConnection(connection, workspaceHandle.schemaName());

            Double estimatedCost = executionMode == ExecutionMode.SELECT
                    ? estimateQueryCost(socketId, connection, trimmedSql)
                    : null;
            int normalizedPage = normalizeExecutionPage(page);
            int normalizedPageSize = normalizeExecutionPageSize(pageSize);
            long startTime = System.nanoTime();
            QueryExecutionResult executionResult = switch (executionMode) {
                case SELECT -> executeSelectPage(
                        socketId,
                        connection,
                        trimmedSql,
                        problemId,
                        estimatedCost,
                        normalizedPage,
                        normalizedPageSize
                );
                case EXPLAIN -> executeExplain(socketId, connection, trimmedSql, problemId, "explain");
                case EXPLAIN_ANALYZE -> executeExplain(socketId, connection, trimmedSql, problemId, "explain_analyze");
                case INDEX_COMMAND -> executeIndexCommand(socketId, connection, trimmedSql, problemId);
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
                                                DbmsType dbmsType,
                                                Consumer<ProblemSubmitProgress> progressListener) {
        LocalDateTime submittedAt = LocalDateTime.now();
        String submittedProblemId = normalizeProblemId(problemId);
        String submittedSql = normalizeSubmittedSql(sql);

        try {
            progressListener.accept(ProblemSubmitProgress.running(submittedProblemId, "submit", "SQL 제출 중"));
            QueryExecutionResult submissionResult = executeSubmittedSqlWithRetry(
                    userId,
                    socketId,
                    submittedProblemId,
                    submittedSql,
                    dbmsType,
                    progressListener
            );
            progressListener.accept(ProblemSubmitProgress.success(submittedProblemId, "submit", "SQL 제출 완료"));

            progressListener.accept(ProblemSubmitProgress.running(submittedProblemId, "answer", "정답 확인 중"));
            if (!isCorrectAnswer(submittedProblemId, submissionResult.rows())) {
                progressListener.accept(ProblemSubmitProgress.incorrect(submittedProblemId, "answer", "오답"));
                saveProblemSubmitHistory(
                        submittedProblemId,
                        userId,
                        dbmsType,
                        submittedSql,
                        false,
                        "오답",
                        submissionResult.executionTimeMs(),
                        submissionResult.cost(),
                        submissionResult.rowCount(),
                        submittedAt
                );
                return ProblemSubmitResult.failure(submittedProblemId, "오답");
            }

            progressListener.accept(ProblemSubmitProgress.success(submittedProblemId, "answer", "정답"));
            progressListener.accept(ProblemSubmitProgress.running(submittedProblemId, "cost", "Cost 확인 중"));
            QueryExecutionResult explainAnalyzeResult = executeExplainAnalyzeWithRetry(
                    userId,
                    socketId,
                    submittedProblemId,
                    submittedSql,
                    dbmsType,
                    progressListener
            );
            progressListener.accept(ProblemSubmitProgress.success(
                    submittedProblemId,
                    "cost",
                    "Cost " + formatCost(explainAnalyzeResult.cost())
            ));

            progressListener.accept(ProblemSubmitProgress.running(submittedProblemId, "plan", "실행계획요소 확인 중"));
            PlanAnalysisResult planAnalysisResult = analyzePostgreSqlPlan(explainAnalyzeResult.planLines(), submittedSql);
            progressListener.accept(ProblemSubmitProgress.success(
                    submittedProblemId,
                    "plan",
                    "실행계획요소 확인 완료",
                    planAnalysisResult.summaryLines()
            ));

            saveProblemSubmitHistory(
                    submittedProblemId,
                    userId,
                    dbmsType,
                    submittedSql,
                    true,
                    "정답",
                    submissionResult.executionTimeMs(),
                    explainAnalyzeResult.cost(),
                    submissionResult.rowCount(),
                    submittedAt
            );
            saveProblemTopHistory(
                    submittedProblemId,
                    userId,
                    dbmsType,
                    submittedSql,
                    resolveSubmittedExecutionTimeMs(submissionResult, explainAnalyzeResult),
                    explainAnalyzeResult.cost(),
                    0,
                    planAnalysisResult.executionPlanElement(),
                    submittedAt
            );

            return ProblemSubmitResult.success(submittedProblemId, "정답");
        } catch (Exception exception) {
            String message = resolveProblemSubmitErrorMessage(exception);

            progressListener.accept(ProblemSubmitProgress.error(submittedProblemId, "submit", message));
            saveProblemSubmitHistory(
                    submittedProblemId,
                    userId,
                    dbmsType,
                    submittedSql,
                    false,
                    message,
                    0,
                    null,
                    0,
                    submittedAt
            );

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

    private QueryExecutionResult executeSelectPage(String socketId,
                                                   Connection connection,
                                                   String sql,
                                                   String problemId,
                                                   Double cost,
                                                   int page,
                                                   int pageSize) throws SQLException {
        long rowCount = fetchSelectRowCount(socketId, connection, sql);
        int totalPages = Math.max(1, (int) Math.ceil((double) rowCount / pageSize));
        int normalizedPage = Math.min(page, totalPages);
        SelectPageResult selectPageResult = fetchSelectPage(socketId, connection, sql, normalizedPage, pageSize);

        return QueryExecutionResult.selectWithCost(
                problemId,
                selectPageResult.columns(),
                selectPageResult.rows(),
                rowCount,
                normalizedPage,
                pageSize,
                cost
        );
    }

    private QueryExecutionResult executeSelectAll(String socketId,
                                                  Connection connection,
                                                  String sql,
                                                  String problemId,
                                                  Double cost) throws SQLException {
        Statement statement = createTrackedStatement(socketId, connection);
        try (statement) {
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

                return QueryExecutionResult.selectWithCost(problemId, columns, rows, rowCount, 1, (int) rowCount, cost);
            }
        } finally {
            clearTrackedStatement(socketId, statement);
        }
    }

    private long fetchSelectRowCount(String socketId, Connection connection, String sql) throws SQLException {
        Statement statement = createTrackedStatement(socketId, connection);
        try (statement) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            statement.execute("SELECT COUNT(*) FROM (" + sql + ") execution_result_count");

            try (ResultSet resultSet = statement.getResultSet()) {
                if (resultSet == null || !resultSet.next()) {
                    return 0;
                }

                return resultSet.getLong(1);
            }
        } finally {
            clearTrackedStatement(socketId, statement);
        }
    }

    private SelectPageResult fetchSelectPage(String socketId,
                                             Connection connection,
                                             String sql,
                                             int page,
                                             int pageSize) throws SQLException {
        PreparedStatement statement = createTrackedPreparedStatement(
                socketId,
                connection,
                "SELECT * FROM (" + sql + ") execution_result_page LIMIT ? OFFSET ?"
        );

        try (statement) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            statement.setInt(1, pageSize);
            statement.setLong(2, (long) (page - 1) * pageSize);
            statement.execute();

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
                while (resultSet.next()) {
                    List<String> row = new ArrayList<>();
                    for (int columnIndex = 1; columnIndex <= metaData.getColumnCount(); columnIndex++) {
                        row.add(String.valueOf(resultSet.getObject(columnIndex)));
                    }
                    rows.add(row);
                }

                return new SelectPageResult(columns, rows);
            }
        } finally {
            clearTrackedStatement(socketId, statement);
        }
    }

    private QueryExecutionResult executeExplain(String socketId,
                                                Connection connection,
                                                String sql,
                                                String problemId,
                                                String mode) throws SQLException {
        Statement statement = createTrackedStatement(socketId, connection);
        try (statement) {
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
        } finally {
            clearTrackedStatement(socketId, statement);
        }
    }

    private QueryExecutionResult executeIndexCommand(String socketId,
                                                     Connection connection,
                                                     String sql,
                                                     String problemId) throws SQLException {
        Statement statement = createTrackedStatement(socketId, connection);
        try (statement) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            statement.execute(sql);
            SQLWarning warning = statement.getWarnings();
            String message = warning != null && warning.getMessage() != null && !warning.getMessage().isBlank()
                    ? warning.getMessage()
                    : "";

            return QueryExecutionResult.command(problemId, message);
        } finally {
            clearTrackedStatement(socketId, statement);
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
                throw new IllegalArgumentException("데이터를 수정하는 CTE는 지원하지 않는다.");
            }

            return ExecutionMode.SELECT;
        }

        throw new IllegalArgumentException("SELECT, EXPLAIN, EXPLAIN ANALYZE, CREATE INDEX, DROP INDEX만 실행할 수 있다.");
    }
    private QueryExecutionResult executeSubmittedSqlWithRetry(String userId,
                                                              String socketId,
                                                              String problemId,
                                                              String sql,
                                                              DbmsType dbmsType,
                                                              Consumer<ProblemSubmitProgress> progressListener) {
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= CONNECTION_RETRY_COUNT; attempt++) {
            try {
                return executeSubmittedSqlOnce(userId, socketId, problemId, sql, dbmsType);
            } catch (RuntimeException exception) {
                lastException = exception;
                if (!isRetriableConnectionError(exception) || attempt == CONNECTION_RETRY_COUNT) {
                    throw exception;
                }

                progressListener.accept(ProblemSubmitProgress.running(problemId, "submit", "DB 커넥션 연결 오류. 2초 후 재시도"));
                sleepForRetryDelay();
            }
        }

        throw lastException != null ? lastException : new IllegalStateException("제출 SQL 실행에 실패했다.");
    }

    private QueryExecutionResult executeExplainAnalyzeWithRetry(String userId,
                                                                String socketId,
                                                                String problemId,
                                                                String sql,
                                                                DbmsType dbmsType,
                                                                Consumer<ProblemSubmitProgress> progressListener) {
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= CONNECTION_RETRY_COUNT; attempt++) {
            try {
                return executeExplainAnalyzeOnce(userId, socketId, problemId, sql, dbmsType);
            } catch (RuntimeException exception) {
                lastException = exception;
                if (!isRetriableConnectionError(exception) || attempt == CONNECTION_RETRY_COUNT) {
                    throw exception;
                }

                progressListener.accept(ProblemSubmitProgress.running(problemId, "cost", "DB 커넥션 연결 오류. 2초 후 재시도"));
                sleepForRetryDelay();
            }
        }

        throw lastException != null ? lastException : new IllegalStateException("실행 계획 확인에 실패했다.");
    }

    private QueryExecutionResult executeSubmittedSqlOnce(String userId,
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
            connection.setAutoCommit(false);
            configureExecutionConnection(connection, workspaceHandle.schemaName());

            Double estimatedCost = estimateQueryCost(socketId, connection, sql);
            long startTime = System.nanoTime();
            QueryExecutionResult executionResult = executeSelectAll(socketId, connection, sql, problemId, estimatedCost);

            connection.commit();
            problemWorkspaceService.markActivity(socketId);

            return executionResult.withExecutionTimeMs(Duration.ofNanos(System.nanoTime() - startTime).toMillis());
        } catch (SQLException exception) {
            throw new IllegalStateException(resolveSqlErrorMessage(exception), exception);
        }
    }

    private QueryExecutionResult executeExplainAnalyzeOnce(String userId,
                                                           String socketId,
                                                           String problemId,
                                                           String sql,
                                                           DbmsType dbmsType) {
        if (dbmsType != DbmsType.POSTGRESQL) {
            throw new IllegalArgumentException("제출은 PostgreSQL만 지원한다.");
        }

        ProblemWorkspaceService.WorkspaceHandle workspaceHandle =
                problemWorkspaceService.prepareWorkspace(userId, problemId, socketId);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            configureExecutionConnection(connection, workspaceHandle.schemaName());

            QueryExecutionResult explainAnalyzeResult = executeExplain(
                    socketId,
                    connection,
                    "EXPLAIN ANALYZE " + sql,
                    problemId,
                    "explain_analyze"
            );
            connection.commit();
            problemWorkspaceService.markActivity(socketId);

            return explainAnalyzeResult.withExecutionTimeMs(extractExplainAnalyzeExecutionTimeMs(explainAnalyzeResult.planLines()));
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

    private int normalizeExecutionPage(Integer page) {
        if (page == null || page < 1) {
            return 1;
        }

        return page;
    }

    private int normalizeExecutionPageSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_SELECT_PAGE_SIZE;
        }

        return Math.min(pageSize, DEFAULT_SELECT_PAGE_SIZE);
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

    private Double estimateQueryCost(String socketId, Connection connection, String sql) throws SQLException {
        Statement statement = createTrackedStatement(socketId, connection);
        try (statement) {
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
        } finally {
            clearTrackedStatement(socketId, statement);
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

    private boolean isCorrectAnswer(String problemId, List<List<String>> rows) {
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException("문제 정보를 찾을 수 없다."));

        if (problem.getAnswer() == null || problem.getAnswer().isBlank()) {
            throw new IllegalStateException("정답 해시가 등록되지 않았다.");
        }

        return problem.getAnswer().equalsIgnoreCase(ProblemAnswerHashSupport.hashRows(rows));
    }

    private void saveProblemSubmitHistory(String problemId,
                                          String userId,
                                          DbmsType dbmsType,
                                          String submittedSql,
                                          boolean success,
                                          String message,
                                          long executionTimeMs,
                                          Double cost,
                                          long rowCount,
                                          LocalDateTime submittedAt) {
        problemSubmitHistoryRepository.save(ProblemSubmitHistory.create(
                problemId,
                userId,
                dbmsType,
                submittedSql,
                success,
                message,
                executionTimeMs,
                cost != null ? cost : 0d,
                rowCount,
                submittedAt
        ));
    }
