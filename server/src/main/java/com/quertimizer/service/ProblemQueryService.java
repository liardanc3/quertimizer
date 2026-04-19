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
    private static final Pattern ALTER_INDEX_PATTERN =
            Pattern.compile("^ALTER\\s+INDEX\\b", Pattern.CASE_INSENSITIVE);
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
        String storedSubmittedSql = preserveSubmittedSql(sql);
        String submittedSql = normalizeSubmittedSql(sql);

        try {
            List<SubmittedStatement> submittedStatements = parseSubmittedStatements(submittedSql);
            SubmittedStatement referenceStatement = resolveReferenceStatement(submittedStatements);
            List<SubmittedStatement> ddlStatements = resolveDdlStatements(submittedStatements);

            for (SubmittedStatement statement : submittedStatements) {
                validateSqlStatement(statement.sql(), userId);
            }

            progressListener.accept(ProblemSubmitProgress.running(submittedProblemId, "validate", "SQL 오류 검사 중"));
            try {
                validateSubmittedSqlWithRetry(
                        userId,
                        socketId,
                        submittedProblemId,
                        referenceStatement.sql(),
                        dbmsType,
                        progressListener
                );
            } catch (Exception exception) {
                String message = resolveProblemSubmitErrorMessage(exception);
                progressListener.accept(ProblemSubmitProgress.error(
                        submittedProblemId,
                        "validate",
                        "SQL 오류 검사 실패",
                        List.of(message)
                ));
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
                        0L,
                        submittedAt
                );
                return ProblemSubmitResult.failure(submittedProblemId, message);
            }
            progressListener.accept(ProblemSubmitProgress.success(submittedProblemId, "validate", "SQL 오류 검사 성공"));

            progressListener.accept(ProblemSubmitProgress.running(submittedProblemId, "answer", "출력 데이터 검사 중"));
            SubmittedExecutionContext submittedExecutionContext;
            try {
                submittedExecutionContext = executeSubmittedSqlWithRetry(
                        userId,
                        socketId,
                        submittedProblemId,
                        submittedStatements,
                        dbmsType,
                        progressListener
                );
            } catch (Exception exception) {
                String message = resolveProblemSubmitErrorMessage(exception);
                progressListener.accept(ProblemSubmitProgress.error(
                        submittedProblemId,
                        "answer",
                        "출력 데이터 검사 실패",
                        List.of(message)
                ));
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
                        0L,
                        submittedAt
                );
                return ProblemSubmitResult.failure(submittedProblemId, message);
            }

            QueryExecutionResult submissionResult = submittedExecutionContext.referenceResult();
            if (!isCorrectAnswer(submittedProblemId, submissionResult.rows())) {
                progressListener.accept(ProblemSubmitProgress.incorrect(submittedProblemId, "answer", "출력 데이터 오답"));
                saveProblemSubmitHistory(
                        submittedProblemId,
                        userId,
                        dbmsType,
                        storedSubmittedSql,
                        false,
                        "오답",
                        submissionResult.executionTimeMs(),
                        submissionResult.cost(),
                        submissionResult.rowCount(),
                        0L,
                        submittedAt
                );
                return ProblemSubmitResult.failure(submittedProblemId, "오답");
            }

            progressListener.accept(ProblemSubmitProgress.success(submittedProblemId, "answer", "출력 데이터 정답"));
            String ddlFailureMessage = null;
            progressListener.accept(ProblemSubmitProgress.running(submittedProblemId, "ddl", "인덱스 변경 반영 중"));
            try {
                List<String> ddlDetailLines = executeSubmittedDdlWithRetry(
                        userId,
                        socketId,
                        submittedProblemId,
                        ddlStatements,
                        dbmsType,
                        progressListener
                );
                progressListener.accept(ProblemSubmitProgress.success(
                        submittedProblemId,
                        "ddl",
                        ddlDetailLines.isEmpty() ? "인덱스 변경내용 없음" : "인덱스 변경 반영 완료",
                        ddlDetailLines
                ));
            } catch (SubmittedDdlExecutionException exception) {
                ddlFailureMessage = exception.getMessage();
                progressListener.accept(ProblemSubmitProgress.error(
                        submittedProblemId,
                        "ddl",
                        "인덱스 변경 반영 실패",
                        exception.detailLines()
                ));
            } catch (Exception exception) {
                ddlFailureMessage = resolveProblemSubmitErrorMessage(exception);
                progressListener.accept(ProblemSubmitProgress.error(
                        submittedProblemId,
                        "ddl",
                        "인덱스 변경 반영 실패",
                        List.of(ddlFailureMessage)
                ));
            }

            progressListener.accept(ProblemSubmitProgress.running(submittedProblemId, "plan", "실행계획 분석 중"));
            QueryExecutionResult explainAnalyzeResult;
            PlanAnalysisResult planAnalysisResult;
            try {
                explainAnalyzeResult = executeExplainAnalyzeWithRetry(
                        userId,
                        socketId,
                        submittedProblemId,
                        submittedExecutionContext.referenceSql(),
                        dbmsType,
                        progressListener
                );
                planAnalysisResult = analyzePostgreSqlPlan(explainAnalyzeResult.planLines(), submittedExecutionContext.referenceSql());
                progressListener.accept(ProblemSubmitProgress.success(
                        submittedProblemId,
                        "plan",
                        "실행계획 분석 성공",
                        buildPlanProgressDetailLines(explainAnalyzeResult, planAnalysisResult)
                ));
            } catch (Exception exception) {
                String message = resolveProblemSubmitErrorMessage(exception);
                progressListener.accept(ProblemSubmitProgress.error(
                        submittedProblemId,
                        "plan",
                        "실행계획 분석 실패",
                        List.of(message)
                ));
                saveProblemSubmitHistory(
                        submittedProblemId,
                        userId,
                        dbmsType,
                        storedSubmittedSql,
                        false,
                        message,
                        submissionResult.executionTimeMs(),
                        null,
                        submissionResult.rowCount(),
                        0L,
                        submittedAt
                );
                return ProblemSubmitResult.failure(submittedProblemId, message);
            }

            long submittedExecutionTimeMs = resolveSubmittedExecutionTimeMs(submissionResult, explainAnalyzeResult);
            boolean submitSucceeded = ddlFailureMessage == null;
            String submitMessage = submitSucceeded ? "정답" : ddlFailureMessage;

            saveProblemSubmitHistory(
                    submittedProblemId,
                    userId,
                    dbmsType,
                    storedSubmittedSql,
                    submitSucceeded,
                    submitMessage,
                    submissionResult.executionTimeMs(),
                    explainAnalyzeResult.cost(),
                    submissionResult.rowCount(),
                    planAnalysisResult.executionPlanElement(),
                    submittedAt
            );

            if (submitSucceeded) {
                saveProblemTopHistory(
                        submittedProblemId,
                        userId,
                        dbmsType,
                        storedSubmittedSql,
                        submittedExecutionTimeMs,
                        explainAnalyzeResult.cost(),
                        0,
                        planAnalysisResult.executionPlanElement(),
                        submittedAt
                );

                return ProblemSubmitResult.success(
                        submittedProblemId,
                        "정답",
                        submittedExecutionTimeMs
                );
            }

            return ProblemSubmitResult.failure(submittedProblemId, submitMessage);
        } catch (Exception exception) {
            String message = resolveProblemSubmitErrorMessage(exception);

            progressListener.accept(ProblemSubmitProgress.error(
                    submittedProblemId,
                    "validate",
                    "SQL 오류 검사 실패",
                    List.of(message)
            ));
            saveProblemSubmitHistory(
                    submittedProblemId,
                    userId,
                    dbmsType,
                    storedSubmittedSql,
                    false,
                    message,
                    0,
                    null,
                    0,
                    0L,
                    submittedAt
            );

            return ProblemSubmitResult.failure(submittedProblemId, message);
        }
    }

    public void cancelInteractiveExecution(String socketId) {
        Statement activeStatement = activeStatements.remove(socketId);
        if (activeStatement == null) {
            return;
        }

        try {
            activeStatement.cancel();
        } catch (SQLException ignored) {
        }

        try {
            activeStatement.close();
        } catch (SQLException ignored) {
        }
    }

    private void validateSql(String sql, String userId) {
        validateSqlText(sql);

        if (MULTI_STATEMENT_PATTERN.matcher(sql).find()) {
            throw new IllegalArgumentException("한 번에 하나의 SQL만 실행할 수 있다.");
        }

        validateSqlStatement(sql, userId);
    }

    private void validateSqlText(String sql) {
        if (sql.isBlank()) {
            throw new IllegalArgumentException("실행할 SQL을 입력해라.");
        }

        if (sql.length() > MAX_SQL_LENGTH) {
            throw new IllegalArgumentException("SQL 길이 제한을 초과했다.");
        }
    }

    private void validateSqlStatement(String sql, String userId) {
        validateSqlText(sql);
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

        if (CREATE_INDEX_PATTERN.matcher(normalizedSql).find()
                || DROP_INDEX_PATTERN.matcher(normalizedSql).find()
                || ALTER_INDEX_PATTERN.matcher(normalizedSql).find()) {
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

        throw new IllegalArgumentException("SELECT, EXPLAIN, EXPLAIN ANALYZE, CREATE INDEX, DROP INDEX, ALTER INDEX만 실행할 수 있다.");
    }

    private void validateSubmittedSqlWithRetry(String userId,
                                               String socketId,
                                               String problemId,
                                               String sql,
                                               DbmsType dbmsType,
                                               Consumer<ProblemSubmitProgress> progressListener) {
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= CONNECTION_RETRY_COUNT; attempt++) {
            try {
                validateSubmittedSqlOnce(userId, socketId, problemId, sql, dbmsType);
                return;
            } catch (RuntimeException exception) {
                lastException = exception;
                if (!isRetriableConnectionError(exception) || attempt == CONNECTION_RETRY_COUNT) {
                    throw exception;
                }

                progressListener.accept(ProblemSubmitProgress.running(problemId, "validate", "DB 커넥션 연결 오류. 2초 후 재시도"));
                sleepForRetryDelay();
            }
        }

        throw lastException != null ? lastException : new IllegalStateException("SQL 오류 검사에 실패했다.");
    }

    private SubmittedExecutionContext executeSubmittedSqlWithRetry(String userId,
                                                                  String socketId,
                                                                  String problemId,
                                                                  List<SubmittedStatement> statements,
                                                                  DbmsType dbmsType,
                                                                  Consumer<ProblemSubmitProgress> progressListener) {
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= CONNECTION_RETRY_COUNT; attempt++) {
            try {
                return executeSubmittedSqlOnce(userId, socketId, problemId, statements, dbmsType);
            } catch (RuntimeException exception) {
                lastException = exception;
                if (!isRetriableConnectionError(exception) || attempt == CONNECTION_RETRY_COUNT) {
                    throw exception;
                }

                progressListener.accept(ProblemSubmitProgress.running(problemId, "answer", "DB 커넥션 연결 오류. 2초 후 재시도"));
                sleepForRetryDelay();
            }
        }

        throw lastException != null ? lastException : new IllegalStateException("출력 데이터 검사에 실패했다.");
    }

    private List<String> executeSubmittedDdlWithRetry(String userId,
                                                      String socketId,
                                                      String problemId,
                                                      List<SubmittedStatement> ddlStatements,
                                                      DbmsType dbmsType,
                                                      Consumer<ProblemSubmitProgress> progressListener) {
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= CONNECTION_RETRY_COUNT; attempt++) {
            try {
                return executeSubmittedDdlOnce(userId, socketId, problemId, ddlStatements, dbmsType);
            } catch (SubmittedDdlExecutionException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                lastException = exception;
                if (!isRetriableConnectionError(exception) || attempt == CONNECTION_RETRY_COUNT) {
                    throw exception;
                }

                progressListener.accept(ProblemSubmitProgress.running(problemId, "ddl", "DB 커넥션 연결 오류. 2초 후 재시도"));
                sleepForRetryDelay();
            }
        }

        throw lastException != null ? lastException : new IllegalStateException("인덱스 변경 반영에 실패했다.");
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

                progressListener.accept(ProblemSubmitProgress.running(problemId, "plan", "DB 커넥션 연결 오류. 2초 후 재시도"));
                sleepForRetryDelay();
            }
        }

        throw lastException != null ? lastException : new IllegalStateException("실행계획 분석에 실패했다.");
    }

    private void validateSubmittedSqlOnce(String userId,
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

            String preparedStatementName = "qt_submit_validate_" + System.nanoTime();
            Statement statement = createTrackedStatement(socketId, connection);
            try (statement) {
                statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                statement.execute("PREPARE " + preparedStatementName + " AS " + sql);
                statement.execute("DEALLOCATE " + preparedStatementName);
            } finally {
                clearTrackedStatement(socketId, statement);
            }

            connection.commit();
            problemWorkspaceService.markActivity(socketId);
        } catch (SQLException exception) {
            throw new IllegalStateException(resolveSqlErrorMessage(exception), exception);
        }
    }

    private SubmittedExecutionContext executeSubmittedSqlOnce(String userId,
                                                              String socketId,
                                                              String problemId,
                                                              List<SubmittedStatement> statements,
                                                              DbmsType dbmsType) {
        if (dbmsType != DbmsType.POSTGRESQL) {
            throw new IllegalArgumentException("제출은 PostgreSQL만 지원한다.");
        }

        SubmittedStatement referenceStatement = resolveReferenceStatement(statements);
        ProblemWorkspaceService.WorkspaceHandle workspaceHandle =
                problemWorkspaceService.prepareWorkspace(userId, problemId, socketId);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            configureExecutionConnection(connection, workspaceHandle.schemaName());

            long startTime = System.nanoTime();
            QueryExecutionResult referenceResult = executeSelectAll(socketId, connection, referenceStatement.sql(), problemId, null)
                    .withExecutionTimeMs(Duration.ofNanos(System.nanoTime() - startTime).toMillis());

            connection.commit();
            problemWorkspaceService.markActivity(socketId);

            return new SubmittedExecutionContext(referenceResult, referenceStatement.sql());
        } catch (SQLException exception) {
            throw new IllegalStateException(resolveSqlErrorMessage(exception), exception);
        }
    }

    private List<String> executeSubmittedDdlOnce(String userId,
                                                 String socketId,
                                                 String problemId,
                                                 List<SubmittedStatement> ddlStatements,
                                                 DbmsType dbmsType) {
        if (dbmsType != DbmsType.POSTGRESQL) {
            throw new IllegalArgumentException("제출은 PostgreSQL만 지원한다.");
        }

        if (ddlStatements.isEmpty()) {
            return List.of();
        }

        ProblemWorkspaceService.WorkspaceHandle workspaceHandle =
                problemWorkspaceService.prepareWorkspace(userId, problemId, socketId);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            configureExecutionConnection(connection, workspaceHandle.schemaName());

            List<String> detailLines = new ArrayList<>();
            for (SubmittedStatement ddlStatement : ddlStatements) {
                try {
                    executeIndexCommand(socketId, connection, ddlStatement.sql(), problemId);
                    detailLines.add("✓ " + buildSubmittedDdlPreview(ddlStatement.sql()));
                } catch (SQLException | RuntimeException exception) {
                    String message = exception instanceof SQLException sqlException
                            ? resolveSqlErrorMessage(sqlException)
                            : resolveProblemSubmitErrorMessage(exception);
                    throw new SubmittedDdlExecutionException(message, List.of(message), exception);
                }
            }

            connection.commit();
            problemWorkspaceService.markActivity(socketId);
            return List.copyOf(detailLines);
        } catch (SubmittedDdlExecutionException exception) {
            throw exception;
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

    private String preserveSubmittedSql(String sql) {
        return sql != null ? sql.replace("\r\n", "\n") : "";
    }

    private List<SubmittedStatement> parseSubmittedStatements(String sql) {
        validateSqlText(sql);

        List<String> normalizedStatements = splitSqlStatements(sql);
        List<SubmittedStatement> statements = new ArrayList<>();
        int referenceStatementIndex = -1;

        for (String statementSql : normalizedStatements) {
            ExecutionMode executionMode = resolveSubmitExecutionMode(statementSql);
            int statementIndex = statements.size();
            if (executionMode == ExecutionMode.SELECT) {
                if (referenceStatementIndex >= 0) {
                    throw new IllegalArgumentException("제출은 SELECT 1개만 가능하다.");
                }
                referenceStatementIndex = statementIndex;
            } else if (referenceStatementIndex >= 0) {
                throw new IllegalArgumentException("제출에서는 SELECT 아래 구문을 함께 보낼 수 없다.");
            }

            statements.add(new SubmittedStatement(
                    createSubmittedStatementKey(statementIndex),
                    statementIndex,
                    statementSql,
                    executionMode,
                    false
            ));
        }

        if (statements.isEmpty()) {
            throw new IllegalArgumentException("제출할 SQL을 입력해라.");
        }

        if (referenceStatementIndex < 0) {
            throw new IllegalArgumentException("제출에는 최소 한 개의 SELECT가 필요하다.");
        }

        List<SubmittedStatement> resolvedStatements = new ArrayList<>();
        for (SubmittedStatement statement : statements) {
            resolvedStatements.add(new SubmittedStatement(
                    statement.key(),
                    statement.index(),
                    statement.sql(),
                    statement.mode(),
                    statement.index() == referenceStatementIndex
            ));
        }

        return resolvedStatements;
    }

    private List<String> splitSqlStatements(String sql) {
        List<String> statements = new ArrayList<>();
        int statementStart = 0;
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int index = 0; index < sql.length(); index++) {
            char currentChar = sql.charAt(index);
            char nextChar = index + 1 < sql.length() ? sql.charAt(index + 1) : '\0';

            if (inLineComment) {
                if (currentChar == '\n') {
                    inLineComment = false;
                }
                continue;
            }

            if (inBlockComment) {
                if (currentChar == '*' && nextChar == '/') {
                    inBlockComment = false;
                    index++;
                }
                continue;
            }

            if (inSingleQuote) {
                if (currentChar == '\'' && nextChar == '\'') {
                    index++;
                    continue;
                }

                if (currentChar == '\'') {
                    inSingleQuote = false;
                }
                continue;
            }

            if (inDoubleQuote) {
                if (currentChar == '"' && nextChar == '"') {
                    index++;
                    continue;
                }

                if (currentChar == '"') {
                    inDoubleQuote = false;
                }
                continue;
            }

            if (currentChar == '-' && nextChar == '-') {
                inLineComment = true;
                index++;
                continue;
            }

            if (currentChar == '/' && nextChar == '*') {
                inBlockComment = true;
                index++;
                continue;
            }

            if (currentChar == '\'') {
                inSingleQuote = true;
                continue;
            }

            if (currentChar == '"') {
                inDoubleQuote = true;
                continue;
            }

            if (currentChar == ';') {
                collectSqlStatement(sql, statementStart, index + 1, statements);
                statementStart = index + 1;
            }
        }

        collectSqlStatement(sql, statementStart, sql.length(), statements);
        return statements;
    }

    private SubmittedStatement resolveReferenceStatement(List<SubmittedStatement> statements) {
        for (SubmittedStatement statement : statements) {
            if (statement.referenceSelect()) {
                return statement;
            }
        }

        throw new IllegalArgumentException("제출 기준 SELECT를 찾을 수 없다.");
    }

    private List<SubmittedStatement> resolveDdlStatements(List<SubmittedStatement> statements) {
        List<SubmittedStatement> ddlStatements = new ArrayList<>();
        for (SubmittedStatement statement : statements) {
            if (statement.mode() == ExecutionMode.INDEX_COMMAND) {
                ddlStatements.add(statement);
            }
        }

        return List.copyOf(ddlStatements);
    }

    private void collectSqlStatement(String sql, int statementStart, int statementEnd, List<String> statements) {
        String rawStatement = sql.substring(statementStart, statementEnd);
        int firstContentOffset = findFirstContentOffset(rawStatement);
        if (firstContentOffset < 0) {
            return;
        }

        int trailingWhitespaceLength = rawStatement.length() - rawStatement.stripTrailing().length();
        String normalizedStatement = trimTrailingSemicolon(
                sql.substring(statementStart + firstContentOffset, statementEnd - trailingWhitespaceLength)
        ).trim();

        if (!normalizedStatement.isBlank()) {
            statements.add(normalizedStatement);
        }
    }

    private int findFirstContentOffset(String sql) {
        for (int index = 0; index < sql.length(); index++) {
            if (!Character.isWhitespace(sql.charAt(index))) {
                return index;
            }
        }

        return -1;
    }

    private ExecutionMode resolveSubmitExecutionMode(String sql) {
        ExecutionMode executionMode = resolveExecutionMode(sql);
        if (executionMode == ExecutionMode.EXPLAIN || executionMode == ExecutionMode.EXPLAIN_ANALYZE) {
            throw new IllegalArgumentException("제출은 SELECT 1개와 INDEX DDL만 지원한다.");
        }

        return executionMode;
    }

    private String createSubmittedStatementKey(int statementIndex) {
        return "submit-statement-" + statementIndex;
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
                                          long executionPlanElement,
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
                executionPlanElement,
                submittedAt
        ));
    }

    private void saveProblemTopHistory(String problemId,
                                       String userId,
                                       DbmsType dbmsType,
                                       String submittedSql,
                                       long executionTimeMs,
                                       Double cost,
                                       long scanRows,
                                       long executionPlanElement,
                                       LocalDateTime submittedAt) {
        if (cost == null) {
            return;
        }

        Optional<ProblemSolveHistory> currentTopHistory =
                problemSolveHistoryRepository.findById(new ProblemSolveHistoryId(problemId, userId));

        if (currentTopHistory.isPresent()
                && !isBetterProblemTopHistory(currentTopHistory.get(), cost, executionTimeMs, submittedAt)) {
            return;
        }

        problemSolveHistoryRepository.save(ProblemSolveHistory.create(
                problemId,
                userId,
                dbmsType,
                submittedSql,
                executionTimeMs,
                cost,
                scanRows,
                executionPlanElement,
                submittedAt
        ));
        problemStore.loadProblems();
    }

    private boolean isBetterProblemTopHistory(ProblemSolveHistory currentHistory,
                                              double candidateCost,
                                              long candidateExecutionTimeMs,
                                              LocalDateTime candidateSubmittedAt) {
        if (candidateCost < currentHistory.getCost()) {
            return true;
        }

        if (candidateCost > currentHistory.getCost()) {
            return false;
        }

        if (candidateExecutionTimeMs < currentHistory.getExecutionTimeMs()) {
            return true;
        }

        if (candidateExecutionTimeMs > currentHistory.getExecutionTimeMs()) {
            return false;
        }

        return candidateSubmittedAt.isBefore(currentHistory.getSubmittedAt());
    }

    private long resolveSubmittedExecutionTimeMs(QueryExecutionResult submissionResult,
                                                 QueryExecutionResult explainAnalyzeResult) {
        if (submissionResult.executionTimeMs() > 0) {
            return submissionResult.executionTimeMs();
        }

        return Math.max(explainAnalyzeResult.executionTimeMs(), 0L);
    }

    private String formatCost(Double cost) {
        return cost == null ? "-" : "%.1f".formatted(cost);
    }

    private List<String> buildPlanProgressDetailLines(QueryExecutionResult explainAnalyzeResult,
                                                      PlanAnalysisResult planAnalysisResult) {
        List<String> detailLines = new ArrayList<>();
        detailLines.add("Cost · " + formatCost(explainAnalyzeResult.cost()));
        detailLines.addAll(resolvePlanElementDetailLines(planAnalysisResult.executionPlanElement()));

        if (detailLines.size() == 1) {
            for (String summaryLine : planAnalysisResult.summaryLines()) {
                detailLines.add("✓ " + summaryLine);
            }
        }

        return List.copyOf(detailLines);
    }

    private List<String> resolvePlanElementDetailLines(long executionPlanElement) {
        LinkedHashSet<String> detailLines = new LinkedHashSet<>();

        if (hasAnyPlanElement(
                executionPlanElement,
                PostgreSqlExecutionPlanElementIndex.FULL_SCAN,
                PostgreSqlExecutionPlanElementIndex.SEQ_SCAN
        )) {
            detailLines.add("✓ Scan · Full Scan");
        }

        if (hasAnyPlanElement(
                executionPlanElement,
                PostgreSqlExecutionPlanElementIndex.INDEX_SCAN,
                PostgreSqlExecutionPlanElementIndex.INDEX_ONLY_SCAN
        )) {
            detailLines.add("✓ Scan · Index Scan");
        }

        if (hasAnyPlanElement(
                executionPlanElement,
                PostgreSqlExecutionPlanElementIndex.BITMAP_INDEX_SCAN,
                PostgreSqlExecutionPlanElementIndex.BITMAP_HEAP_SCAN
        )) {
            detailLines.add("✓ Scan · Bitmap Scan");
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.TID_SCAN)) {
            detailLines.add("✓ Scan · Tid Scan");
        }

        if (hasAnyPlanElement(
                executionPlanElement,
                PostgreSqlExecutionPlanElementIndex.SUBQUERY_SCAN,
                PostgreSqlExecutionPlanElementIndex.CTE_SCAN,
                PostgreSqlExecutionPlanElementIndex.FUNCTION_SCAN,
                PostgreSqlExecutionPlanElementIndex.VALUES_SCAN
        )) {
            detailLines.add("✓ Scan · Derived Scan");
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.NESTED_LOOP)) {
            detailLines.add("✓ Join · Nested Loop");
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.MERGE_JOIN)) {
            detailLines.add("✓ Join · Merge Join");
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.HASH_JOIN)) {
            detailLines.add("✓ Join · Hash Join");
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.INDEX_CONDITION)) {
            detailLines.add("✓ Filter · Access Filter");
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.FILTER)) {
            detailLines.add("✓ Filter · Post Filter");
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.SORT)) {
            detailLines.add("✓ Sort · Plain Sort");
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.INCREMENTAL_SORT)) {
            detailLines.add("✓ Sort · Incremental Sort");
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.HASH_AGGREGATE)) {
            detailLines.add("✓ Aggregate · Hash Agg");
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.GROUP_AGGREGATE)) {
            detailLines.add("✓ Aggregate · Group Agg");
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.UNIQUE)) {
            detailLines.add("✓ Aggregate · Unique Agg");
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.HINT)) {
            detailLines.add("✓ Hint · Used");
        }

        return List.copyOf(detailLines);
    }

    private boolean hasAnyPlanElement(long executionPlanElement, int... indexes) {
        for (int index : indexes) {
            if (hasPlanElement(executionPlanElement, index)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasPlanElement(long executionPlanElement, int index) {
        return (executionPlanElement & (1L << index)) != 0;
    }

    private PlanAnalysisResult analyzePostgreSqlPlan(List<String> planLines, String submittedSql) {
        long executionPlanElement = 0L;
        LinkedHashSet<String> summaryLines = new LinkedHashSet<>();

        if (submittedSql != null && submittedSql.contains("/*+")) {
            executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.HINT);
            summaryLines.add("힌트를 사용했다.");
        }

        for (String planLine : planLines) {
            if (planLine == null || planLine.isBlank()) {
                continue;
            }

            String normalizedLine = planLine.trim().toUpperCase(Locale.ROOT);

            if (normalizedLine.contains("SEQ SCAN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.FULL_SCAN);
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.SEQ_SCAN);
                summaryLines.add("Seq Scan이 포함되었다.");
            }

            if (normalizedLine.contains("INDEX ONLY SCAN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.INDEX_ONLY_SCAN);
                summaryLines.add("Index Only Scan이 포함되었다.");
            } else if (normalizedLine.contains("BITMAP INDEX SCAN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.BITMAP_INDEX_SCAN);
                summaryLines.add("Bitmap Index Scan이 포함되었다.");
            } else if (normalizedLine.contains("BITMAP HEAP SCAN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.BITMAP_HEAP_SCAN);
                summaryLines.add("Bitmap Heap Scan이 포함되었다.");
            } else if (normalizedLine.contains("INDEX SCAN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.INDEX_SCAN);
                summaryLines.add("Index Scan이 포함되었다.");
            }

            if (normalizedLine.contains("TID SCAN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.TID_SCAN);
                summaryLines.add("Tid Scan이 포함되었다.");
            }

            if (normalizedLine.contains("SUBQUERY SCAN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.SUBQUERY_SCAN);
                summaryLines.add("Subquery Scan이 포함되었다.");
            }

            if (normalizedLine.contains("CTE SCAN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.CTE_SCAN);
                summaryLines.add("CTE Scan이 포함되었다.");
            }

            if (normalizedLine.contains("FUNCTION SCAN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.FUNCTION_SCAN);
                summaryLines.add("Function Scan이 포함되었다.");
            }

            if (normalizedLine.contains("VALUES SCAN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.VALUES_SCAN);
                summaryLines.add("Values Scan이 포함되었다.");
            }

            if (normalizedLine.contains("HASH JOIN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.HASH_JOIN);
                summaryLines.add("Hash Join이 포함되었다.");
            }

            if (normalizedLine.contains("MERGE JOIN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.MERGE_JOIN);
                summaryLines.add("Merge Join이 포함되었다.");
            }

            if (normalizedLine.contains("NESTED LOOP")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.NESTED_LOOP);
                summaryLines.add("Nested Loop가 포함되었다.");
            }

            if (containsAny(normalizedLine, Set.of("HASHAGGREGATE", "HASH AGGREGATE"))) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.HASH_AGGREGATE);
                summaryLines.add("Hash Aggregate가 포함되었다.");
            }

            if (containsAny(normalizedLine, Set.of("GROUPAGGREGATE", "GROUP AGGREGATE"))) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.GROUP_AGGREGATE);
                summaryLines.add("Group Aggregate가 포함되었다.");
            }

            if (normalizedLine.contains("INCREMENTAL SORT")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.INCREMENTAL_SORT);
                summaryLines.add("Incremental Sort가 포함되었다.");
            } else if (normalizedLine.contains("SORT")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.SORT);
                summaryLines.add("Sort가 포함되었다.");
            }

            if (normalizedLine.contains("LIMIT")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.LIMIT);
                summaryLines.add("Limit이 포함되었다.");
            }

            if (normalizedLine.contains("UNIQUE") && !normalizedLine.contains("INNER UNIQUE")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.UNIQUE);
                summaryLines.add("Unique가 포함되었다.");
            }

            if (normalizedLine.contains("MATERIALIZE")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.MATERIALIZE);
                summaryLines.add("Materialize가 포함되었다.");
            }

            if (normalizedLine.contains("MEMOIZE")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.MEMOIZE);
                summaryLines.add("Memoize가 포함되었다.");
            }

            if (normalizedLine.contains("MERGE APPEND")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.MERGE_APPEND);
                summaryLines.add("Merge Append가 포함되었다.");
            } else if (normalizedLine.contains("APPEND")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.APPEND);
                summaryLines.add("Append가 포함되었다.");
            }

            if (normalizedLine.contains("GATHER MERGE")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.GATHER_MERGE);
                summaryLines.add("Gather Merge가 포함되었다.");
            } else if (normalizedLine.contains("GATHER")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.GATHER);
                summaryLines.add("Gather가 포함되었다.");
            }

            if (normalizedLine.contains("PARALLEL")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.PARALLEL);
                summaryLines.add("병렬 실행이 포함되었다.");
            }

            if (containsAny(normalizedLine, Set.of("PARTITION PRUNING", "PARTITIONS REMOVED"))) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.PARTITION_PRUNING);
                summaryLines.add("파티션 가지치기가 포함되었다.");
            }

            if (containsAny(normalizedLine, Set.of("INDEX COND:", "RECHECK COND:"))) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.INDEX_CONDITION);
                summaryLines.add("인덱스 접근 조건이 포함되었다.");
            }

            if (containsAny(normalizedLine, Set.of("FILTER:", "ROWS REMOVED BY FILTER", "JOIN FILTER:"))) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.FILTER);
                summaryLines.add("후처리 필터가 포함되었다.");
            }
        }

        if (summaryLines.isEmpty()) {
            summaryLines.add("대표 실행계획 요소를 찾지 못했다.");
        }

        return new PlanAnalysisResult(executionPlanElement, List.copyOf(summaryLines));
    }

    private long appendPlanElement(long executionPlanElement, int index) {
        return executionPlanElement | (1L << index);
    }

    private boolean containsAny(String normalizedLine, Set<String> tokens) {
        for (String token : tokens) {
            if (normalizedLine.contains(token)) {
                return true;
            }
        }

        return false;
    }

    private long extractExplainAnalyzeExecutionTimeMs(List<String> planLines) {
        Double executionTimeMs = null;

        for (String planLine : planLines) {
            if (planLine == null || planLine.isBlank()) {
                continue;
            }

            Matcher matcher = PLAN_EXECUTION_TIME_PATTERN.matcher(planLine);
            if (matcher.find()) {
                try {
                    executionTimeMs = Double.parseDouble(matcher.group(1));
                } catch (NumberFormatException ignored) {
                    return 0L;
                }
            }
        }

        return executionTimeMs != null ? Math.round(executionTimeMs) : 0L;
    }

    private boolean isRetriableConnectionError(RuntimeException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof SQLException sqlException) {
                String sqlState = sqlException.getSQLState();
                if (sqlState != null && sqlState.startsWith("08")) {
                    return true;
                }

                String message = sqlException.getMessage();
                if (message != null) {
                    String normalizedMessage = message.toLowerCase(Locale.ROOT);
                    if (containsAny(normalizedMessage, Set.of(
                            "connection refused",
                            "connection reset",
                            "connection is closed",
                            "broken pipe",
                            "communications link failure",
                            "the connection attempt failed",
                            "terminating connection"
                    ))) {
                        return true;
                    }
                }
            }

            cause = cause.getCause();
        }

        return false;
    }

    private void sleepForRetryDelay() {
        try {
            Thread.sleep(CONNECTION_RETRY_DELAY_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("재시도 대기 중 인터럽트가 발생했다.", exception);
        }
    }

    private Statement createTrackedStatement(String socketId, Connection connection) throws SQLException {
        Statement statement = connection.createStatement();
        activeStatements.put(socketId, statement);
        return statement;
    }

    private PreparedStatement createTrackedPreparedStatement(String socketId,
                                                             Connection connection,
                                                             String sql) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        activeStatements.put(socketId, statement);
        return statement;
    }

    private void clearTrackedStatement(String socketId, Statement statement) {
        activeStatements.remove(socketId, statement);
    }

    private String resolveProblemSubmitErrorMessage(Exception exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof SQLException sqlException) {
                return resolveSqlErrorMessage(sqlException);
            }

            if (cause.getMessage() != null && !cause.getMessage().isBlank()) {
                return cause.getMessage();
            }

            cause = cause.getCause();
        }

        return "SQL 제출에 실패했다.";
    }

    private String resolveSqlErrorMessage(SQLException exception) {
        String message = exception.getMessage();
        String sqlState = exception.getSQLState();
        String normalizedMessage = message != null ? message.toLowerCase(Locale.ROOT) : "";

        if ("57014".equals(sqlState)) {
            if (normalizedMessage.contains("statement timeout")) {
                return "SQL 실행 시간이 제한을 초과했다.";
            }

            if (normalizedMessage.contains("user request")) {
                return "SQL 실행을 중지했다.";
            }

            return "SQL 실행이 취소되었다.";
        }

        return message != null && !message.isBlank()
                ? message
                : "SQL 실행에 실패했다.";
    }

    private String buildSubmittedDdlPreview(String sql) {
        String preview = sql.replaceAll("\\s+", " ").trim();
        if (preview.length() <= 20) {
            return preview;
        }

        return preview.substring(0, 20) + "...";
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
                                       Integer currentPage,
                                       Integer pageSize,
                                       long executionTimeMs,
                                       Double cost) {

        private static QueryExecutionResult selectWithCost(String problemId,
                                                           List<String> columns,
                                                           List<List<String>> rows,
                                                           long rowCount,
                                                           Integer currentPage,
                                                           Integer pageSize,
                                                           Double cost) {
            return new QueryExecutionResult(
                    problemId,
                    "select",
                    "조회 결과를 반환했다.",
                    columns,
                    rows,
                    List.of(),
                    rowCount,
                    currentPage,
                    pageSize,
                    0,
                    cost
            );
        }

        private static QueryExecutionResult planWithCost(String problemId,
                                                         String mode,
                                                         List<String> planLines,
                                                         Double cost) {
            return new QueryExecutionResult(
                    problemId,
                    mode,
                    "실행 계획을 반환했다.",
                    List.of(),
                    List.of(),
                    planLines,
                    planLines.size(),
                    null,
                    null,
                    0,
                    cost
            );
        }

        private static QueryExecutionResult command(String problemId, String message) {
            return new QueryExecutionResult(
                    problemId,
                    "command",
                    message != null && !message.isBlank() ? message : "명령을 실행했다.",
                    List.of(),
                    List.of(),
                    List.of(),
                    0,
                    null,
                    null,
                    0,
                    null
            );
        }

        private QueryExecutionResult withExecutionTimeMs(long executionTimeMs) {
            return new QueryExecutionResult(
                    problemId,
                    mode,
                    message,
                    columns,
                    rows,
                    planLines,
                    rowCount,
                    currentPage,
                    pageSize,
                    executionTimeMs,
                    cost
            );
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

    public record ProblemSubmitProgress(String problemId,
                                        String stepKey,
                                        String status,
                                        String message,
                                        List<String> detailLines,
                                        String statementKey,
                                        Integer statementIndex,
                                        String statementSql,
                                        String statementMode,
                                        Boolean statementReference) {

        private static ProblemSubmitProgress running(String problemId, String stepKey, String message) {
            return new ProblemSubmitProgress(problemId, stepKey, "running", message, List.of(), null, null, null, null, null);
        }

        private static ProblemSubmitProgress success(String problemId, String stepKey, String message) {
            return new ProblemSubmitProgress(problemId, stepKey, "success", message, List.of(), null, null, null, null, null);
        }

        private static ProblemSubmitProgress success(String problemId, String stepKey, String message, List<String> detailLines) {
            return new ProblemSubmitProgress(problemId, stepKey, "success", message, detailLines != null ? detailLines : List.of(), null, null, null, null, null);
        }

        private static ProblemSubmitProgress incorrect(String problemId, String stepKey, String message) {
            return new ProblemSubmitProgress(problemId, stepKey, "incorrect", message, List.of(), null, null, null, null, null);
        }

        private static ProblemSubmitProgress error(String problemId, String stepKey, String message) {
            return new ProblemSubmitProgress(problemId, stepKey, "error", message, List.of(), null, null, null, null, null);
        }

        private static ProblemSubmitProgress error(String problemId, String stepKey, String message, List<String> detailLines) {
            return new ProblemSubmitProgress(problemId, stepKey, "error", message, detailLines != null ? detailLines : List.of(), null, null, null, null, null);
        }

        private static ProblemSubmitProgress runningStatement(String problemId, SubmittedStatement statement, String message) {
            return statement(problemId, statement, "running", message);
        }

        private static ProblemSubmitProgress successStatement(String problemId, SubmittedStatement statement, String message) {
            return statement(problemId, statement, "success", message);
        }

        private static ProblemSubmitProgress errorStatement(String problemId, SubmittedStatement statement, String message) {
            return statement(problemId, statement, "error", message);
        }

        private static ProblemSubmitProgress statement(String problemId, SubmittedStatement statement, String status, String message) {
            return new ProblemSubmitProgress(
                    problemId,
                    statement.key(),
                    status,
                    message,
                    List.of(),
                    statement.key(),
                    statement.index(),
                    statement.sql(),
                    statement.mode() == ExecutionMode.SELECT ? "select" : "command",
                    statement.referenceSelect()
            );
        }
    }

    private record SubmittedStatement(String key,
                                      int index,
                                      String sql,
                                      ExecutionMode mode,
                                      boolean referenceSelect) {
    }

    private record SubmittedExecutionContext(QueryExecutionResult referenceResult, String referenceSql) {
    }

    private record SelectPageResult(List<String> columns, List<List<String>> rows) {
    }

    private record PlanAnalysisResult(long executionPlanElement, List<String> summaryLines) {
    }

    private static final class SubmittedDdlExecutionException extends RuntimeException {

        private final List<String> detailLines;

        private SubmittedDdlExecutionException(String message, List<String> detailLines, Throwable cause) {
            super(message, cause);
            this.detailLines = detailLines != null ? detailLines : List.of();
        }

        private List<String> detailLines() {
            return detailLines;
        }
    }
}
