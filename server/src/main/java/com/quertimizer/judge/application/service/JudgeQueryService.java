package com.quertimizer.judge.application.service;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.global.constant.MySqlExecutionPlanElementIndex;
import com.quertimizer.global.constant.PostgreSqlExecutionPlanElementIndex;
import com.quertimizer.judge.application.port.DbmsSqlDialect;
import com.quertimizer.judge.application.port.DbmsSqlDialectProvider;
import com.quertimizer.judge.domain.model.JudgeSqlExecutionMode;
import com.quertimizer.judge.domain.policy.JudgeSqlStatementPolicy;
import com.quertimizer.judge.domain.service.JudgeAnswerHashSupport;
import com.quertimizer.judge.domain.service.JudgeSqlStatementParser;
import com.quertimizer.problem.domain.entity.Problem;
import com.quertimizer.problem.domain.entity.ProblemSolveHistory;
import com.quertimizer.problem.domain.entity.ProblemSolveHistoryId;
import com.quertimizer.problem.domain.entity.ProblemSubmitHistory;
import com.quertimizer.problem.application.port.ProblemRepository;
import com.quertimizer.problem.application.port.ProblemSolveHistoryRepository;
import com.quertimizer.problem.application.port.ProblemSubmitHistoryRepository;
import com.quertimizer.problem.application.store.ProblemStore;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

import static com.quertimizer.problem.domain.model.ProblemQueryFailReason.*;
import static com.quertimizer.problem.domain.model.ProblemPlanSummaryText.*;
import static com.quertimizer.problem.domain.model.ProblemQueryResultText.*;
import static com.quertimizer.problem.domain.model.ProblemSubmitProgressStep.*;
import static com.quertimizer.problem.domain.model.ProblemSubmitProgressText.*;

@Service
@RequiredArgsConstructor
public class JudgeQueryService {

    private static final int QUERY_TIMEOUT_SECONDS = 60;
    private static final int CONNECTION_RETRY_COUNT = 3;
    private static final long CONNECTION_RETRY_DELAY_MS = 2_000L;
    private static final int DEFAULT_SELECT_PAGE_SIZE = 10;
    private static final int MAX_RESULT_COLUMNS = 100;
    private static final int MAX_SUBMIT_RESULT_ROWS = 10_000;

    private static final Pattern PLAN_TOTAL_COST_PATTERN =
            Pattern.compile("cost=[0-9]+(?:\\.[0-9]+)?\\.\\.([0-9]+(?:\\.[0-9]+)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MYSQL_QUERY_COST_PATTERN =
            Pattern.compile("\"query_cost\"\\s*:\\s*\"?([0-9]+(?:\\.[0-9]+)?)\"?", Pattern.CASE_INSENSITIVE);
    private static final Pattern PLAN_EXECUTION_TIME_PATTERN =
            Pattern.compile("Execution Time:\\s*([0-9]+(?:\\.[0-9]+)?)\\s*ms", Pattern.CASE_INSENSITIVE);

    private final JudgeWorkspaceService judgeWorkspaceService;
    private final DbmsSqlDialectProvider dbmsSqlDialectProvider;
    private final JudgeSqlStatementPolicy judgeSqlStatementPolicy;
    private final JudgeSqlStatementParser judgeSqlStatementParser;
    private final ProblemSubmitHistoryRepository problemSubmitHistoryRepository;
    private final ProblemSolveHistoryRepository problemSolveHistoryRepository;
    private final ProblemRepository problemRepository;
    private final ProblemStore problemStore;
    private final ConcurrentHashMap<String, Statement> activeStatements = new ConcurrentHashMap<>();

    public QueryExecutionResult executeInteractiveSql(String handle,
                                                      String socketId,
                                                      String problemId,
                                                      String sql,
                                                      DbmsType dbmsType,
                                                      Integer page,
                                                      Integer pageSize) {
        String trimmedSql = trimTrailingSemicolon(sql);
        judgeSqlStatementPolicy.validateInteractiveSql(trimmedSql, handle);
        JudgeSqlExecutionMode executionMode = judgeSqlStatementPolicy.resolveExecutionMode(trimmedSql);

        try (JudgeWorkspaceService.WorkspaceSession workspaceSession =
                     judgeWorkspaceService.openWorkspace(handle, problemId, socketId, dbmsType)) {
            Connection connection = workspaceSession.connection();
            connection.setAutoCommit(false);
            configureExecutionConnection(connection, dbmsType, workspaceSession.schemaName());

            Double estimatedCost = executionMode == JudgeSqlExecutionMode.SELECT
                    ? estimateQueryCost(socketId, connection, dbmsType, trimmedSql)
                    : null;
            int normalizedPage = normalizeExecutionPage(page);
            int normalizedPageSize = normalizeExecutionPageSize(pageSize);
            long startTime = System.nanoTime();
            QueryExecutionResult executionResult = switch (executionMode) {
                case SELECT -> executeSelectPage(
                        socketId,
                        connection,
                        dbmsType,
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
            judgeWorkspaceService.markActivity(socketId);

            return executionResult.withExecutionTimeMs(Duration.ofNanos(System.nanoTime() - startTime).toMillis());
        } catch (SQLException exception) {
            throw new IllegalStateException(resolveSqlErrorMessage(exception), exception);
        }
    }

    @Transactional
    public ProblemSubmitResult submitProblemSql(String handle,
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
                judgeSqlStatementPolicy.validateSqlStatement(statement.sql(), handle);
            }

            progressListener.accept(ProblemSubmitProgress.running(submittedProblemId, VALIDATE.getKey(), SQL_VALIDATE_RUNNING.getText()));
            try {
                validateSubmittedSqlWithRetry(
                        handle,
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
                        VALIDATE.getKey(),
                        SQL_VALIDATE_FAILED.getText(),
                        List.of(message)
                ));
                saveProblemSubmitHistory(
                        submittedProblemId,
                        handle,
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
            progressListener.accept(ProblemSubmitProgress.success(submittedProblemId, VALIDATE.getKey(), SQL_VALIDATE_SUCCESS.getText()));

            progressListener.accept(ProblemSubmitProgress.running(submittedProblemId, ANSWER.getKey(), ANSWER_VALIDATE_RUNNING.getText()));
            SubmittedExecutionContext submittedExecutionContext;
            try {
                submittedExecutionContext = executeSubmittedSqlWithRetry(
                        handle,
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
                        ANSWER.getKey(),
                        ANSWER_VALIDATE_FAILED.getText(),
                        List.of(message)
                ));
                saveProblemSubmitHistory(
                        submittedProblemId,
                        handle,
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
            if (!isCorrectAnswer(submittedProblemId, submissionResult.columns(), submissionResult.rows())) {
                progressListener.accept(ProblemSubmitProgress.incorrect(submittedProblemId, ANSWER.getKey(), ANSWER_INCORRECT.getText()));
                saveProblemSubmitHistory(
                        submittedProblemId,
                        handle,
                        dbmsType,
                        storedSubmittedSql,
                        false,
                        INCORRECT_ANSWER.getText(),
                        submissionResult.executionTimeMs(),
                        submissionResult.cost(),
                        submissionResult.rowCount(),
                        0L,
                        submittedAt
                );
                return ProblemSubmitResult.failure(submittedProblemId, INCORRECT_ANSWER.getText());
            }

            progressListener.accept(ProblemSubmitProgress.success(submittedProblemId, ANSWER.getKey(), ANSWER_CORRECT.getText()));
            String ddlFailureMessage = null;
            progressListener.accept(ProblemSubmitProgress.running(submittedProblemId, DDL.getKey(), DDL_RUNNING.getText()));
            try {
                List<String> ddlDetailLines = executeSubmittedDdlWithRetry(
                        handle,
                        socketId,
                        submittedProblemId,
                        ddlStatements,
                        dbmsType,
                        progressListener
                );
                progressListener.accept(ProblemSubmitProgress.success(
                        submittedProblemId,
                        DDL.getKey(),
                        ddlDetailLines.isEmpty() ? DDL_EMPTY.getText() : DDL_SUCCESS.getText(),
                        ddlDetailLines
                ));
            } catch (SubmittedDdlExecutionException exception) {
                ddlFailureMessage = exception.getMessage();
                progressListener.accept(ProblemSubmitProgress.error(
                        submittedProblemId,
                        DDL.getKey(),
                        DDL_FAILED.getText(),
                        exception.detailLines()
                ));
            } catch (Exception exception) {
                ddlFailureMessage = resolveProblemSubmitErrorMessage(exception);
                progressListener.accept(ProblemSubmitProgress.error(
                        submittedProblemId,
                        DDL.getKey(),
                        DDL_FAILED.getText(),
                        List.of(ddlFailureMessage)
                ));
            }

            progressListener.accept(ProblemSubmitProgress.running(submittedProblemId, PLAN.getKey(), PLAN_RUNNING.getText()));
            QueryExecutionResult explainAnalyzeResult;
            PlanAnalysisResult planAnalysisResult;
            try {
                explainAnalyzeResult = executeExplainAnalyzeWithRetry(
                        handle,
                        socketId,
                        submittedProblemId,
                        submittedExecutionContext.referenceSql(),
                        dbmsType,
                        progressListener
                );
                planAnalysisResult = analyzePlan(dbmsType, explainAnalyzeResult.planLines(), submittedExecutionContext.referenceSql());
                progressListener.accept(ProblemSubmitProgress.success(
                        submittedProblemId,
                        PLAN.getKey(),
                        PLAN_SUCCESS.getText(),
                        buildPlanProgressDetailLines(dbmsType, explainAnalyzeResult, planAnalysisResult)
                ));
            } catch (Exception exception) {
                String message = resolveProblemSubmitErrorMessage(exception);
                progressListener.accept(ProblemSubmitProgress.error(
                        submittedProblemId,
                        PLAN.getKey(),
                        PLAN_FAILED.getText(),
                        List.of(message)
                ));
                saveProblemSubmitHistory(
                        submittedProblemId,
                        handle,
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
            String submitMessage = submitSucceeded ? CORRECT_ANSWER.getText() : ddlFailureMessage;

            saveProblemSubmitHistory(
                    submittedProblemId,
                    handle,
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
                        handle,
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
                        CORRECT_ANSWER.getText(),
                        submittedExecutionTimeMs
                );
            }

            return ProblemSubmitResult.failure(submittedProblemId, submitMessage);
        } catch (Exception exception) {
            String message = resolveProblemSubmitErrorMessage(exception);

            progressListener.accept(ProblemSubmitProgress.error(
                    submittedProblemId,
                    VALIDATE.getKey(),
                    SQL_VALIDATE_FAILED.getText(),
                    List.of(message)
            ));
            saveProblemSubmitHistory(
                    submittedProblemId,
                    handle,
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
        // 진행 중인 인터랙티브 실행을 취소
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

    private void configureExecutionConnection(Connection connection, DbmsType dbmsType, String schemaName) throws SQLException {
        // 실행용 세션 설정 적용
        DbmsSqlDialect dialect = dbmsSqlDialectProvider.get(dbmsType);
        try (Statement statement = connection.createStatement()) {
            for (String useSchemaSql : dialect.useSchemaSqls(schemaName)) {
                statement.execute(useSchemaSql);
            }
            for (String timeoutSql : dialect.statementTimeoutSqls(QUERY_TIMEOUT_SECONDS)) {
                statement.execute(timeoutSql);
            }
        }
    }

    private QueryExecutionResult executeSelectPage(String socketId,
                                                   Connection connection,
                                                   DbmsType dbmsType,
                                                   String sql,
                                                   String problemId,
                                                   Double cost,
                                                   int page,
                                                   int pageSize) throws SQLException {
        long rowCount = fetchSelectRowCount(socketId, connection, dbmsType, sql);
        int totalPages = Math.max(1, (int) Math.ceil((double) rowCount / pageSize));
        int normalizedPage = Math.min(page, totalPages);
        SelectPageResult selectPageResult = fetchSelectPage(socketId, connection, dbmsType, sql, normalizedPage, pageSize);

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
                    throw new IllegalArgumentException(SELECT_RESULT_UNAVAILABLE.getMessage());
                }

                ResultSetMetaData metaData = resultSet.getMetaData();
                validateColumnLimit(metaData);
                List<String> columns = new ArrayList<>();
                for (int columnIndex = 1; columnIndex <= metaData.getColumnCount(); columnIndex++) {
                    columns.add(metaData.getColumnLabel(columnIndex));
                }

                List<List<String>> rows = new ArrayList<>();
                long rowCount = 0;
                while (resultSet.next()) {
                    rowCount++;
                    if (rowCount > MAX_SUBMIT_RESULT_ROWS) {
                        throw new IllegalArgumentException("SQL 실행 결과 행 수가 너무 많습니다.");
                    }

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

    private long fetchSelectRowCount(String socketId, Connection connection, DbmsType dbmsType, String sql) throws SQLException {
        // 전체 조회 건수 계산
        Statement statement = createTrackedStatement(socketId, connection);
        try (statement) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            statement.execute(dbmsSqlDialectProvider.get(dbmsType).selectCountSql(sql));

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
                                             DbmsType dbmsType,
                                             String sql,
                                             int page,
                                             int pageSize) throws SQLException {
        PreparedStatement statement = createTrackedPreparedStatement(
                socketId,
                connection,
                dbmsSqlDialectProvider.get(dbmsType).selectPageSql(sql)
        );

        try (statement) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            statement.setInt(1, pageSize);
            statement.setLong(2, (long) (page - 1) * pageSize);
            statement.execute();

            try (ResultSet resultSet = statement.getResultSet()) {
                if (resultSet == null) {
                    throw new IllegalArgumentException(SELECT_RESULT_UNAVAILABLE.getMessage());
                }

                ResultSetMetaData metaData = resultSet.getMetaData();
                validateColumnLimit(metaData);
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
                    throw new IllegalArgumentException(PLAN_RESULT_UNAVAILABLE.getMessage());
                }

                List<String> planLines = readPlanLines(resultSet);

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

    private void validateSubmittedSqlWithRetry(String handle,
                                               String socketId,
                                               String problemId,
                                               String sql,
                                               DbmsType dbmsType,
                                               Consumer<ProblemSubmitProgress> progressListener) {
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= CONNECTION_RETRY_COUNT; attempt++) {
            try {
                validateSubmittedSqlOnce(handle, socketId, problemId, sql, dbmsType);
                return;
            } catch (RuntimeException exception) {
                lastException = exception;
                if (!isRetriableConnectionError(exception) || attempt == CONNECTION_RETRY_COUNT) {
                    throw exception;
                }

                progressListener.accept(ProblemSubmitProgress.running(problemId, VALIDATE.getKey(), CONNECTION_RETRY.getText()));
                sleepForRetryDelay();
            }
        }

        throw lastException != null ? lastException : new IllegalStateException(SQL_VALIDATE_UNEXPECTED_FAILURE.getText());
    }

    private SubmittedExecutionContext executeSubmittedSqlWithRetry(String handle,
                                                                  String socketId,
                                                                  String problemId,
                                                                  List<SubmittedStatement> statements,
                                                                  DbmsType dbmsType,
                                                                  Consumer<ProblemSubmitProgress> progressListener) {
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= CONNECTION_RETRY_COUNT; attempt++) {
            try {
                return executeSubmittedSqlOnce(handle, socketId, problemId, statements, dbmsType);
            } catch (RuntimeException exception) {
                lastException = exception;
                if (!isRetriableConnectionError(exception) || attempt == CONNECTION_RETRY_COUNT) {
                    throw exception;
                }

                progressListener.accept(ProblemSubmitProgress.running(problemId, ANSWER.getKey(), CONNECTION_RETRY.getText()));
                sleepForRetryDelay();
            }
        }

        throw lastException != null ? lastException : new IllegalStateException(ANSWER_VALIDATE_UNEXPECTED_FAILURE.getText());
    }

    private List<String> executeSubmittedDdlWithRetry(String handle,
                                                      String socketId,
                                                      String problemId,
                                                      List<SubmittedStatement> ddlStatements,
                                                      DbmsType dbmsType,
                                                      Consumer<ProblemSubmitProgress> progressListener) {
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= CONNECTION_RETRY_COUNT; attempt++) {
            try {
                return executeSubmittedDdlOnce(handle, socketId, problemId, ddlStatements, dbmsType);
            } catch (SubmittedDdlExecutionException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                lastException = exception;
                if (!isRetriableConnectionError(exception) || attempt == CONNECTION_RETRY_COUNT) {
                    throw exception;
                }

                progressListener.accept(ProblemSubmitProgress.running(problemId, DDL.getKey(), CONNECTION_RETRY.getText()));
                sleepForRetryDelay();
            }
        }

        throw lastException != null ? lastException : new IllegalStateException(DDL_UNEXPECTED_FAILURE.getText());
    }

    private QueryExecutionResult executeExplainAnalyzeWithRetry(String handle,
                                                                String socketId,
                                                                String problemId,
                                                                String sql,
                                                                DbmsType dbmsType,
                                                                Consumer<ProblemSubmitProgress> progressListener) {
        RuntimeException lastException = null;

        for (int attempt = 1; attempt <= CONNECTION_RETRY_COUNT; attempt++) {
            try {
                return executeExplainAnalyzeOnce(handle, socketId, problemId, sql, dbmsType);
            } catch (RuntimeException exception) {
                lastException = exception;
                if (!isRetriableConnectionError(exception) || attempt == CONNECTION_RETRY_COUNT) {
                    throw exception;
                }

                progressListener.accept(ProblemSubmitProgress.running(problemId, PLAN.getKey(), CONNECTION_RETRY.getText()));
                sleepForRetryDelay();
            }
        }

        throw lastException != null ? lastException : new IllegalStateException(PLAN_UNEXPECTED_FAILURE.getText());
    }

    private void validateSubmittedSqlOnce(String handle,
                                          String socketId,
                                          String problemId,
                                          String sql,
                                          DbmsType dbmsType) {
        try (JudgeWorkspaceService.WorkspaceSession workspaceSession =
                     judgeWorkspaceService.openWorkspace(handle, problemId, socketId, dbmsType)) {
            Connection connection = workspaceSession.connection();
            connection.setAutoCommit(false);
            configureExecutionConnection(connection, dbmsType, workspaceSession.schemaName());

            String preparedStatementName = "qt_submit_validate_" + System.nanoTime();
            DbmsSqlDialect dialect = dbmsSqlDialectProvider.get(dbmsType);
            Statement statement = createTrackedStatement(socketId, connection);
            try (statement) {
                statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
                statement.execute(dialect.validateSelectSql(preparedStatementName, sql));
                String cleanupValidatedSelectSql = dialect.cleanupValidatedSelectSql(preparedStatementName);
                if (!cleanupValidatedSelectSql.isBlank()) {
                    statement.execute(cleanupValidatedSelectSql);
                }
            } finally {
                clearTrackedStatement(socketId, statement);
            }

            connection.commit();
            judgeWorkspaceService.markActivity(socketId);
        } catch (SQLException exception) {
            throw new IllegalStateException(resolveSqlErrorMessage(exception), exception);
        }
    }

    private SubmittedExecutionContext executeSubmittedSqlOnce(String handle,
                                                              String socketId,
                                                              String problemId,
                                                              List<SubmittedStatement> statements,
                                                              DbmsType dbmsType) {
        SubmittedStatement referenceStatement = resolveReferenceStatement(statements);

        try (JudgeWorkspaceService.WorkspaceSession workspaceSession =
                     judgeWorkspaceService.openWorkspace(handle, problemId, socketId, dbmsType)) {
            Connection connection = workspaceSession.connection();
            connection.setAutoCommit(false);
            configureExecutionConnection(connection, dbmsType, workspaceSession.schemaName());

            long startTime = System.nanoTime();
            QueryExecutionResult referenceResult = executeSelectAll(socketId, connection, referenceStatement.sql(), problemId, null)
                    .withExecutionTimeMs(Duration.ofNanos(System.nanoTime() - startTime).toMillis());

            connection.commit();
            judgeWorkspaceService.markActivity(socketId);

            return new SubmittedExecutionContext(referenceResult, referenceStatement.sql());
        } catch (SQLException exception) {
            throw new IllegalStateException(resolveSqlErrorMessage(exception), exception);
        }
    }

    private List<String> executeSubmittedDdlOnce(String handle,
                                                 String socketId,
                                                 String problemId,
                                                 List<SubmittedStatement> ddlStatements,
                                                 DbmsType dbmsType) {
        if (ddlStatements.isEmpty()) {
            return List.of();
        }

        try (JudgeWorkspaceService.WorkspaceSession workspaceSession =
                     judgeWorkspaceService.openWorkspace(handle, problemId, socketId, dbmsType)) {
            Connection connection = workspaceSession.connection();
            connection.setAutoCommit(false);
            configureExecutionConnection(connection, dbmsType, workspaceSession.schemaName());

            List<String> detailLines = new ArrayList<>();
            for (SubmittedStatement ddlStatement : ddlStatements) {
                try {
                    executeIndexCommand(socketId, connection, ddlStatement.sql(), problemId);
                    detailLines.add(CHECK_PREFIX.getText() + buildSubmittedDdlPreview(ddlStatement.sql()));
                } catch (SQLException | RuntimeException exception) {
                    String message = exception instanceof SQLException sqlException
                            ? resolveSqlErrorMessage(sqlException)
                            : resolveProblemSubmitErrorMessage(exception);
                    throw new SubmittedDdlExecutionException(message, List.of(message), exception);
                }
            }

            connection.commit();
            judgeWorkspaceService.markActivity(socketId);
            return List.copyOf(detailLines);
        } catch (SubmittedDdlExecutionException exception) {
            throw exception;
        } catch (SQLException exception) {
            throw new IllegalStateException(resolveSqlErrorMessage(exception), exception);
        }
    }

    private QueryExecutionResult executeExplainAnalyzeOnce(String handle,
                                                           String socketId,
                                                           String problemId,
                                                           String sql,
                                                           DbmsType dbmsType) {
        try (JudgeWorkspaceService.WorkspaceSession workspaceSession =
                     judgeWorkspaceService.openWorkspace(handle, problemId, socketId, dbmsType)) {
            Connection connection = workspaceSession.connection();
            connection.setAutoCommit(false);
            configureExecutionConnection(connection, dbmsType, workspaceSession.schemaName());

            QueryExecutionResult explainAnalyzeResult = executeExplain(
                    socketId,
                    connection,
                    dbmsSqlDialectProvider.get(dbmsType).explainAnalyzeSql(sql),
                    problemId,
                    "explain_analyze"
            );
            connection.commit();
            judgeWorkspaceService.markActivity(socketId);

            return explainAnalyzeResult.withExecutionTimeMs(extractExplainAnalyzeExecutionTimeMs(explainAnalyzeResult.planLines()));
        } catch (SQLException exception) {
            throw new IllegalStateException(resolveSqlErrorMessage(exception), exception);
        }
    }

    private String trimTrailingSemicolon(String sql) {
        // 끝 세미콜론 정리
        return sql.trim().replaceFirst(";\\s*$", "");
    }

    private String normalizeProblemId(String problemId) {
        // 문제 번호 정규화
        return problemId != null ? problemId.trim() : "";
    }

    private String normalizeSubmittedSql(String sql) {
        // 제출 SQL 정규화
        return sql != null ? trimTrailingSemicolon(sql) : "";
    }

    private String preserveSubmittedSql(String sql) {
        // 제출 SQL 원문 보존
        return sql != null ? sql.replace("\r\n", "\n") : "";
    }

    private List<SubmittedStatement> parseSubmittedStatements(String sql) {
        // 제출 구문 목록 파싱
        judgeSqlStatementPolicy.validateSqlText(sql);

        List<String> normalizedStatements = judgeSqlStatementParser.splitStatements(sql);
        List<SubmittedStatement> statements = new ArrayList<>();
        int referenceStatementIndex = -1;

        for (String statementSql : normalizedStatements) {
            JudgeSqlExecutionMode executionMode = judgeSqlStatementPolicy.resolveSubmitExecutionMode(statementSql);
            int statementIndex = statements.size();
            if (executionMode == JudgeSqlExecutionMode.SELECT) {
                if (referenceStatementIndex >= 0) {
                    throw new IllegalArgumentException(SUBMIT_SELECT_ONLY.getMessage());
                }
                referenceStatementIndex = statementIndex;
            } else if (referenceStatementIndex >= 0) {
                throw new IllegalArgumentException(SUBMIT_SELECT_FOLLOWED_BY_STATEMENTS.getMessage());
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
            throw new IllegalArgumentException(SUBMIT_SQL_REQUIRED.getMessage());
        }

        if (referenceStatementIndex < 0) {
            throw new IllegalArgumentException(SUBMIT_SELECT_REQUIRED.getMessage());
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

    private SubmittedStatement resolveReferenceStatement(List<SubmittedStatement> statements) {
        // 기준 구문 결정
        for (SubmittedStatement statement : statements) {
            if (statement.referenceSelect()) {
                return statement;
            }
        }

        throw new IllegalArgumentException(SUBMIT_REFERENCE_SELECT_NOT_FOUND.getMessage());
    }

    private List<SubmittedStatement> resolveDdlStatements(List<SubmittedStatement> statements) {
        // DDL 구문 목록 결정
        List<SubmittedStatement> ddlStatements = new ArrayList<>();
        for (SubmittedStatement statement : statements) {
            if (statement.mode() == JudgeSqlExecutionMode.INDEX_COMMAND) {
                ddlStatements.add(statement);
            }
        }

        return List.copyOf(ddlStatements);
    }

    private String createSubmittedStatementKey(int statementIndex) {
        // 제출 구문 키 생성
        return "submit-statement-" + statementIndex;
    }

    private int normalizeExecutionPage(Integer page) {
        // 실행 페이지 정규화
        if (page == null || page < 1) {
            return 1;
        }

        return page;
    }

    private int normalizeExecutionPageSize(Integer pageSize) {
        // 실행 페이지 크기 정규화
        if (pageSize == null || pageSize < 1) {
            return DEFAULT_SELECT_PAGE_SIZE;
        }

        return Math.min(pageSize, DEFAULT_SELECT_PAGE_SIZE);
    }

    private Double estimateQueryCost(String socketId, Connection connection, DbmsType dbmsType, String sql) throws SQLException {
        // 실행 계획 기준 예상 Cost 계산
        Statement statement = createTrackedStatement(socketId, connection);
        try (statement) {
            statement.setQueryTimeout(QUERY_TIMEOUT_SECONDS);
            statement.execute(dbmsSqlDialectProvider.get(dbmsType).explainSql(sql));

            try (ResultSet resultSet = statement.getResultSet()) {
                if (resultSet == null) {
                    return null;
                }

                return extractEstimatedCost(readPlanLines(resultSet));
            }
        } finally {
            clearTrackedStatement(socketId, statement);
        }
    }

    private Double extractEstimatedCost(List<String> planLines) {
        // 실행 계획 예상 Cost 추출
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

            Matcher mysqlMatcher = MYSQL_QUERY_COST_PATTERN.matcher(planLine);
            if (mysqlMatcher.find()) {
                try {
                    return Double.parseDouble(mysqlMatcher.group(1));
                } catch (NumberFormatException ignored) {
                    return null;
                }
            }
        }

        return null;
    }

    private List<String> readPlanLines(ResultSet resultSet) throws SQLException {
        // DBMS별 EXPLAIN 결과를 문자열 목록으로 변환
        ResultSetMetaData metaData = resultSet.getMetaData();
        validateColumnLimit(metaData);
        List<String> planLines = new ArrayList<>();

        while (resultSet.next()) {
            if (metaData.getColumnCount() == 1) {
                planLines.add(String.valueOf(resultSet.getObject(1)));
                continue;
            }

            List<String> values = new ArrayList<>();
            for (int columnIndex = 1; columnIndex <= metaData.getColumnCount(); columnIndex++) {
                Object value = resultSet.getObject(columnIndex);
                values.add(metaData.getColumnLabel(columnIndex) + "=" + (value != null ? value : ""));
            }
            planLines.add(String.join(", ", values));
        }

        return planLines;
    }

    private void validateColumnLimit(ResultSetMetaData metaData) throws SQLException {
        if (metaData.getColumnCount() > MAX_RESULT_COLUMNS) {
            throw new IllegalArgumentException("SQL 실행 결과 컬럼 수가 너무 많습니다.");
        }
    }

    private boolean isCorrectAnswer(String problemId, List<String> columns, List<List<String>> rows) {
        // Correct 정답 여부 확인
        Problem problem = problemRepository.findById(problemId)
                .orElseThrow(() -> new IllegalArgumentException(PROBLEM_INFO_NOT_FOUND.getMessage()));

        if (problem.getAnswer() == null || problem.getAnswer().isBlank()) {
            throw new IllegalStateException(ANSWER_HASH_NOT_REGISTERED.getMessage());
        }

        return JudgeAnswerHashSupport.matches(problem.getAnswer(), columns, rows);
    }

    private void saveProblemSubmitHistory(String problemId,
                                          String handle,
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
                handle,
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
                                       String handle,
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
                problemSolveHistoryRepository.findById(new ProblemSolveHistoryId(problemId, handle));

        if (currentTopHistory.isPresent()
                && !isBetterProblemTopHistory(currentTopHistory.get(), cost, executionTimeMs, submittedAt)) {
            return;
        }

        problemSolveHistoryRepository.save(ProblemSolveHistory.create(
                problemId,
                handle,
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
        // Cost 포맷
        return cost == null ? "-" : "%.1f".formatted(cost);
    }

    private List<String> buildPlanProgressDetailLines(DbmsType dbmsType,
                                                      QueryExecutionResult explainAnalyzeResult,
                                                      PlanAnalysisResult planAnalysisResult) {
        List<String> detailLines = new ArrayList<>();
        detailLines.add(COST_PREFIX.getText() + formatCost(explainAnalyzeResult.cost()));
        detailLines.addAll(resolvePlanElementDetailLines(dbmsType, planAnalysisResult.executionPlanElement()));

        if (detailLines.size() == 1) {
            for (String summaryLine : planAnalysisResult.summaryLines()) {
                detailLines.add(CHECK_PREFIX.getText() + summaryLine);
            }
        }

        return List.copyOf(detailLines);
    }

    private List<String> resolvePlanElementDetailLines(DbmsType dbmsType, long executionPlanElement) {
        // 실행 계획 Element 상세 Lines 결정
        if (dbmsType == DbmsType.MYSQL) {
            return resolveMySqlPlanElementDetailLines(executionPlanElement);
        }

        LinkedHashSet<String> detailLines = new LinkedHashSet<>();

        if (hasAnyPlanElement(
                executionPlanElement,
                PostgreSqlExecutionPlanElementIndex.FULL_SCAN,
                PostgreSqlExecutionPlanElementIndex.SEQ_SCAN
        )) {
            detailLines.add(PLAN_FULL_SCAN.getText());
        }

        if (hasAnyPlanElement(
                executionPlanElement,
                PostgreSqlExecutionPlanElementIndex.INDEX_SCAN,
                PostgreSqlExecutionPlanElementIndex.INDEX_ONLY_SCAN
        )) {
            detailLines.add(PLAN_INDEX_SCAN.getText());
        }

        if (hasAnyPlanElement(
                executionPlanElement,
                PostgreSqlExecutionPlanElementIndex.BITMAP_INDEX_SCAN,
                PostgreSqlExecutionPlanElementIndex.BITMAP_HEAP_SCAN
        )) {
            detailLines.add(PLAN_BITMAP_SCAN.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.TID_SCAN)) {
            detailLines.add(PLAN_TID_SCAN.getText());
        }

        if (hasAnyPlanElement(
                executionPlanElement,
                PostgreSqlExecutionPlanElementIndex.SUBQUERY_SCAN,
                PostgreSqlExecutionPlanElementIndex.CTE_SCAN,
                PostgreSqlExecutionPlanElementIndex.FUNCTION_SCAN,
                PostgreSqlExecutionPlanElementIndex.VALUES_SCAN
        )) {
            detailLines.add(PLAN_DERIVED_SCAN.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.NESTED_LOOP)) {
            detailLines.add(PLAN_NESTED_LOOP.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.MERGE_JOIN)) {
            detailLines.add(PLAN_MERGE_JOIN.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.HASH_JOIN)) {
            detailLines.add(PLAN_HASH_JOIN.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.INDEX_CONDITION)) {
            detailLines.add(PLAN_ACCESS_FILTER.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.FILTER)) {
            detailLines.add(PLAN_POST_FILTER.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.SORT)) {
            detailLines.add(PLAN_PLAIN_SORT.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.INCREMENTAL_SORT)) {
            detailLines.add(PLAN_INCREMENTAL_SORT.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.HASH_AGGREGATE)) {
            detailLines.add(PLAN_HASH_AGG.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.GROUP_AGGREGATE)) {
            detailLines.add(PLAN_GROUP_AGG.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.UNIQUE)) {
            detailLines.add(PLAN_UNIQUE_AGG.getText());
        }

        if (hasPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.HINT)) {
            detailLines.add(PLAN_HINT_USED.getText());
        }

        return List.copyOf(detailLines);
    }

    private List<String> resolveMySqlPlanElementDetailLines(long executionPlanElement) {
        // MySQL 실행 계획 Element 상세 Lines 결정
        LinkedHashSet<String> detailLines = new LinkedHashSet<>();

        if (hasPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.FULL_TABLE_SCAN)) {
            detailLines.add(PLAN_FULL_SCAN.getText());
        }

        if (hasAnyPlanElement(
                executionPlanElement,
                MySqlExecutionPlanElementIndex.INDEX_SCAN,
                MySqlExecutionPlanElementIndex.RANGE_SCAN,
                MySqlExecutionPlanElementIndex.REF_SCAN,
                MySqlExecutionPlanElementIndex.EQ_REF_SCAN,
                MySqlExecutionPlanElementIndex.CONST_SCAN,
                MySqlExecutionPlanElementIndex.INDEX_MERGE
        )) {
            detailLines.add(PLAN_INDEX_SCAN.getText());
        }

        if (hasAnyPlanElement(
                executionPlanElement,
                MySqlExecutionPlanElementIndex.DERIVED_TABLE,
                MySqlExecutionPlanElementIndex.MATERIALIZED_SUBQUERY
        )) {
            detailLines.add(PLAN_DERIVED_SCAN.getText());
        }

        if (hasPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.NESTED_LOOP_JOIN)) {
            detailLines.add(PLAN_NESTED_LOOP.getText());
        }

        if (hasPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.HASH_JOIN)) {
            detailLines.add(PLAN_HASH_JOIN.getText());
        }

        if (hasPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.INDEX_CONDITION)) {
            detailLines.add(PLAN_ACCESS_FILTER.getText());
        }

        if (hasAnyPlanElement(
                executionPlanElement,
                MySqlExecutionPlanElementIndex.FILTER_CONDITION,
                MySqlExecutionPlanElementIndex.ATTACHED_CONDITION
        )) {
            detailLines.add(PLAN_POST_FILTER.getText());
        }

        if (hasPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.FILESORT)) {
            detailLines.add(PLAN_PLAIN_SORT.getText());
        }

        if (hasAnyPlanElement(
                executionPlanElement,
                MySqlExecutionPlanElementIndex.GROUPING_OPERATION,
                MySqlExecutionPlanElementIndex.AGGREGATE
        )) {
            detailLines.add(PLAN_GROUP_AGG.getText());
        }

        if (hasPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.HINT)) {
            detailLines.add(PLAN_HINT_USED.getText());
        }

        return List.copyOf(detailLines);
    }

    private boolean hasAnyPlanElement(long executionPlanElement, int... indexes) {
        // Any 실행 계획 Element 여부 확인
        for (int index : indexes) {
            if (hasPlanElement(executionPlanElement, index)) {
                return true;
            }
        }

        return false;
    }

    private boolean hasPlanElement(long executionPlanElement, int index) {
        // 실행 계획 Element 여부 확인
        return (executionPlanElement & (1L << index)) != 0;
    }

    private PlanAnalysisResult analyzePlan(DbmsType dbmsType, List<String> planLines, String submittedSql) {
        // DBMS별 실행 계획 분석
        return switch (dbmsType) {
            case POSTGRESQL -> analyzePostgreSqlPlan(planLines, submittedSql);
            case MYSQL -> analyzeMySqlPlan(planLines, submittedSql);
        };
    }

    private PlanAnalysisResult analyzePostgreSqlPlan(List<String> planLines, String submittedSql) {
        // analyze Postgre SQL 실행 계획 처리
        long executionPlanElement = 0L;
        LinkedHashSet<String> summaryLines = new LinkedHashSet<>();

        if (submittedSql != null && submittedSql.contains("/*+")) {
            executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.HINT);
            summaryLines.add(HINT_USED.getText());
        }

        for (String planLine : planLines) {
            if (planLine == null || planLine.isBlank()) {
                continue;
            }

            String normalizedLine = planLine.trim().toUpperCase(Locale.ROOT);

            if (normalizedLine.contains("SEQ SCAN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.FULL_SCAN);
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.SEQ_SCAN);
                summaryLines.add(SEQ_SCAN_INCLUDED.getText());
            }

            if (normalizedLine.contains("INDEX ONLY SCAN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.INDEX_ONLY_SCAN);
                summaryLines.add(INDEX_ONLY_SCAN_INCLUDED.getText());
            } else if (normalizedLine.contains("BITMAP INDEX SCAN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.BITMAP_INDEX_SCAN);
                summaryLines.add(BITMAP_INDEX_SCAN_INCLUDED.getText());
            } else if (normalizedLine.contains("BITMAP HEAP SCAN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.BITMAP_HEAP_SCAN);
                summaryLines.add(BITMAP_HEAP_SCAN_INCLUDED.getText());
            } else if (normalizedLine.contains("INDEX SCAN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.INDEX_SCAN);
                summaryLines.add(INDEX_SCAN_INCLUDED.getText());
            }

            if (normalizedLine.contains("TID SCAN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.TID_SCAN);
                summaryLines.add(TID_SCAN_INCLUDED.getText());
            }

            if (normalizedLine.contains("SUBQUERY SCAN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.SUBQUERY_SCAN);
                summaryLines.add(SUBQUERY_SCAN_INCLUDED.getText());
            }

            if (normalizedLine.contains("CTE SCAN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.CTE_SCAN);
                summaryLines.add(CTE_SCAN_INCLUDED.getText());
            }

            if (normalizedLine.contains("FUNCTION SCAN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.FUNCTION_SCAN);
                summaryLines.add(FUNCTION_SCAN_INCLUDED.getText());
            }

            if (normalizedLine.contains("VALUES SCAN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.VALUES_SCAN);
                summaryLines.add(VALUES_SCAN_INCLUDED.getText());
            }

            if (normalizedLine.contains("HASH JOIN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.HASH_JOIN);
                summaryLines.add(HASH_JOIN_INCLUDED.getText());
            }

            if (normalizedLine.contains("MERGE JOIN")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.MERGE_JOIN);
                summaryLines.add(MERGE_JOIN_INCLUDED.getText());
            }

            if (normalizedLine.contains("NESTED LOOP")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.NESTED_LOOP);
                summaryLines.add(NESTED_LOOP_INCLUDED.getText());
            }

            if (containsAny(normalizedLine, Set.of("HASHAGGREGATE", "HASH AGGREGATE"))) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.HASH_AGGREGATE);
                summaryLines.add(HASH_AGGREGATE_INCLUDED.getText());
            }

            if (containsAny(normalizedLine, Set.of("GROUPAGGREGATE", "GROUP AGGREGATE"))) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.GROUP_AGGREGATE);
                summaryLines.add(GROUP_AGGREGATE_INCLUDED.getText());
            }

            if (normalizedLine.contains("INCREMENTAL SORT")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.INCREMENTAL_SORT);
                summaryLines.add(INCREMENTAL_SORT_INCLUDED.getText());
            } else if (normalizedLine.contains("SORT")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.SORT);
                summaryLines.add(SORT_INCLUDED.getText());
            }

            if (normalizedLine.contains("LIMIT")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.LIMIT);
                summaryLines.add(LIMIT_INCLUDED.getText());
            }

            if (normalizedLine.contains("UNIQUE") && !normalizedLine.contains("INNER UNIQUE")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.UNIQUE);
                summaryLines.add(UNIQUE_INCLUDED.getText());
            }

            if (normalizedLine.contains("MATERIALIZE")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.MATERIALIZE);
                summaryLines.add(MATERIALIZE_INCLUDED.getText());
            }

            if (normalizedLine.contains("MEMOIZE")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.MEMOIZE);
                summaryLines.add(MEMOIZE_INCLUDED.getText());
            }

            if (normalizedLine.contains("MERGE APPEND")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.MERGE_APPEND);
                summaryLines.add(MERGE_APPEND_INCLUDED.getText());
            } else if (normalizedLine.contains("APPEND")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.APPEND);
                summaryLines.add(APPEND_INCLUDED.getText());
            }

            if (normalizedLine.contains("GATHER MERGE")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.GATHER_MERGE);
                summaryLines.add(GATHER_MERGE_INCLUDED.getText());
            } else if (normalizedLine.contains("GATHER")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.GATHER);
                summaryLines.add(GATHER_INCLUDED.getText());
            }

            if (normalizedLine.contains("PARALLEL")) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.PARALLEL);
                summaryLines.add(PARALLEL_INCLUDED.getText());
            }

            if (containsAny(normalizedLine, Set.of("PARTITION PRUNING", "PARTITIONS REMOVED"))) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.PARTITION_PRUNING);
                summaryLines.add(PARTITION_PRUNING_INCLUDED.getText());
            }

            if (containsAny(normalizedLine, Set.of("INDEX COND:", "RECHECK COND:"))) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.INDEX_CONDITION);
                summaryLines.add(INDEX_CONDITION_INCLUDED.getText());
            }

            if (containsAny(normalizedLine, Set.of("FILTER:", "ROWS REMOVED BY FILTER", "JOIN FILTER:"))) {
                executionPlanElement = appendPlanElement(executionPlanElement, PostgreSqlExecutionPlanElementIndex.FILTER);
                summaryLines.add(POST_FILTER_INCLUDED.getText());
            }
        }

        if (summaryLines.isEmpty()) {
            summaryLines.add(REPRESENTATIVE_ELEMENT_NOT_FOUND.getText());
        }

        return new PlanAnalysisResult(executionPlanElement, List.copyOf(summaryLines));
    }

    private PlanAnalysisResult analyzeMySqlPlan(List<String> planLines, String submittedSql) {
        // MySQL 실행 계획 처리
        long executionPlanElement = 0L;
        LinkedHashSet<String> summaryLines = new LinkedHashSet<>();

        if (submittedSql != null && submittedSql.contains("/*+")) {
            executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.HINT);
            summaryLines.add(HINT_USED.getText());
        }

        for (String planLine : planLines) {
            if (planLine == null || planLine.isBlank()) {
                continue;
            }

            String normalizedLine = planLine.trim().toUpperCase(Locale.ROOT);

            if (containsAny(normalizedLine, Set.of("TYPE=ALL", "\"ACCESS_TYPE\": \"ALL\"", "\"ACCESS_TYPE\":\"ALL\""))) {
                executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.FULL_TABLE_SCAN);
                summaryLines.add(SEQ_SCAN_INCLUDED.getText());
            }

            if (containsAny(normalizedLine, Set.of("TYPE=INDEX", "\"ACCESS_TYPE\": \"INDEX\"", "\"ACCESS_TYPE\":\"INDEX\""))) {
                executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.INDEX_SCAN);
                summaryLines.add(INDEX_SCAN_INCLUDED.getText());
            }

            if (containsAny(normalizedLine, Set.of("TYPE=RANGE", "\"ACCESS_TYPE\": \"RANGE\"", "\"ACCESS_TYPE\":\"RANGE\""))) {
                executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.RANGE_SCAN);
                summaryLines.add(INDEX_SCAN_INCLUDED.getText());
            }

            if (containsAny(normalizedLine, Set.of("TYPE=REF", "\"ACCESS_TYPE\": \"REF\"", "\"ACCESS_TYPE\":\"REF\""))) {
                executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.REF_SCAN);
                summaryLines.add(INDEX_SCAN_INCLUDED.getText());
            }

            if (containsAny(normalizedLine, Set.of("TYPE=EQ_REF", "\"ACCESS_TYPE\": \"EQ_REF\"", "\"ACCESS_TYPE\":\"EQ_REF\""))) {
                executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.EQ_REF_SCAN);
                summaryLines.add(INDEX_SCAN_INCLUDED.getText());
            }

            if (containsAny(normalizedLine, Set.of("TYPE=CONST", "\"ACCESS_TYPE\": \"CONST\"", "\"ACCESS_TYPE\":\"CONST\""))) {
                executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.CONST_SCAN);
                summaryLines.add(INDEX_SCAN_INCLUDED.getText());
            }

            if (containsAny(normalizedLine, Set.of("INDEX_MERGE", "INDEX MERGE"))) {
                executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.INDEX_MERGE);
                summaryLines.add(INDEX_SCAN_INCLUDED.getText());
            }

            if (containsAny(normalizedLine, Set.of("DERIVED", "DEPENDENT DERIVED"))) {
                executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.DERIVED_TABLE);
                summaryLines.add(SUBQUERY_SCAN_INCLUDED.getText());
            }

            if (normalizedLine.contains("MATERIALIZED")) {
                executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.MATERIALIZED_SUBQUERY);
                summaryLines.add(MATERIALIZE_INCLUDED.getText());
            }

            if (containsAny(normalizedLine, Set.of("NESTED_LOOP", "NESTED LOOP"))) {
                executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.NESTED_LOOP_JOIN);
                summaryLines.add(NESTED_LOOP_INCLUDED.getText());
            }

            if (containsAny(normalizedLine, Set.of("HASH_JOIN", "HASH JOIN"))) {
                executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.HASH_JOIN);
                summaryLines.add(HASH_JOIN_INCLUDED.getText());
            }

            if (containsAny(normalizedLine, Set.of("USING INDEX CONDITION", "INDEX_CONDITION", "INDEX CONDITION"))) {
                executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.INDEX_CONDITION);
                summaryLines.add(INDEX_CONDITION_INCLUDED.getText());
            }

            if (containsAny(normalizedLine, Set.of("USING WHERE", "FILTER_CONDITION", "ATTACHED_CONDITION", "ATTACHED CONDITION"))) {
                executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.FILTER_CONDITION);
                summaryLines.add(POST_FILTER_INCLUDED.getText());
            }

            if (containsAny(normalizedLine, Set.of("USING FILESORT", "FILESORT"))) {
                executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.FILESORT);
                summaryLines.add(SORT_INCLUDED.getText());
            }

            if (containsAny(normalizedLine, Set.of("USING TEMPORARY", "TEMPORARY_TABLE", "TEMPORARY TABLE"))) {
                executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.TEMPORARY_TABLE);
                summaryLines.add(MATERIALIZE_INCLUDED.getText());
            }

            if (containsAny(normalizedLine, Set.of("GROUPING_OPERATION", "GROUPING OPERATION", "GROUP BY"))) {
                executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.GROUPING_OPERATION);
                summaryLines.add(GROUP_AGGREGATE_INCLUDED.getText());
            }

            if (containsAny(normalizedLine, Set.of("WINDOWING", "WINDOW_OPERATION", "WINDOW OPERATION"))) {
                executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.WINDOW_OPERATION);
                summaryLines.add(GROUP_AGGREGATE_INCLUDED.getText());
            }

            if (normalizedLine.contains("AGGREGATE")) {
                executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.AGGREGATE);
                summaryLines.add(GROUP_AGGREGATE_INCLUDED.getText());
            }

            if (normalizedLine.contains("LIMIT")) {
                executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.LIMIT);
                summaryLines.add(LIMIT_INCLUDED.getText());
            }

            if (containsAny(normalizedLine, Set.of("USING JOIN BUFFER", "JOIN_BUFFER", "JOIN BUFFER"))) {
                executionPlanElement = appendPlanElement(executionPlanElement, MySqlExecutionPlanElementIndex.USING_JOIN_BUFFER);
                summaryLines.add(NESTED_LOOP_INCLUDED.getText());
            }
        }

        if (summaryLines.isEmpty()) {
            summaryLines.add(REPRESENTATIVE_ELEMENT_NOT_FOUND.getText());
        }

        return new PlanAnalysisResult(executionPlanElement, List.copyOf(summaryLines));
    }

    private long appendPlanElement(long executionPlanElement, int index) {
        // append 실행 계획 Element 처리
        return executionPlanElement | (1L << index);
    }

    private boolean containsAny(String normalizedLine, Set<String> tokens) {
        // contains Any 처리
        for (String token : tokens) {
            if (normalizedLine.contains(token)) {
                return true;
            }
        }

        return false;
    }

    private long extractExplainAnalyzeExecutionTimeMs(List<String> planLines) {
        // Explain Analyze 실행 시간 Ms 추출
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
        // Retriable Connection Error 여부 확인
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
        // 재시도 대기
        try {
            Thread.sleep(CONNECTION_RETRY_DELAY_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(RETRY_WAIT_INTERRUPTED.getMessage(), exception);
        }
    }

    private Statement createTrackedStatement(String socketId, Connection connection) throws SQLException {
        // 실행 추적용 구문 생성
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
        // 실행 추적 구문 정리
        activeStatements.remove(socketId, statement);
    }

    private String resolveProblemSubmitErrorMessage(Exception exception) {
        // 문제 제출 Error 메시지 결정
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
        // SQL Error 메시지 결정
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
        // 제출 DDL Preview 구성
        String preview = sql.replaceAll("\\s+", " ").trim();
        if (preview.length() <= 20) {
            return preview;
        }

        return preview.substring(0, 20) + "...";
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
                    SELECT_RESULT_RETURNED.getText(),
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
                    PLAN_RESULT_RETURNED.getText(),
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
            // command 처리
            return new QueryExecutionResult(
                    problemId,
                    "command",
                    message != null && !message.isBlank() ? message : COMMAND_EXECUTED.getText(),
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
            // with 실행 시간 Ms 처리
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
            // success 처리
            return new ProblemSubmitResult(problemId, true, message, executionTimeMs);
        }

        private static ProblemSubmitResult failure(String problemId, String message) {
            // failure 처리
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
            // running 처리
            return new ProblemSubmitProgress(problemId, stepKey, "running", message, List.of(), null, null, null, null, null);
        }

        private static ProblemSubmitProgress success(String problemId, String stepKey, String message) {
            // success 처리
            return new ProblemSubmitProgress(problemId, stepKey, "success", message, List.of(), null, null, null, null, null);
        }

        private static ProblemSubmitProgress success(String problemId, String stepKey, String message, List<String> detailLines) {
            // success 처리
            return new ProblemSubmitProgress(
                    problemId, stepKey, "success", message,
                    detailLines != null ? detailLines : List.of(),
                    null, null, null, null, null
            );
        }

        private static ProblemSubmitProgress incorrect(String problemId, String stepKey, String message) {
            // incorrect 처리
            return new ProblemSubmitProgress(problemId, stepKey, "incorrect", message, List.of(), null, null, null, null, null);
        }

        private static ProblemSubmitProgress error(String problemId, String stepKey, String message) {
            // error 처리
            return new ProblemSubmitProgress(problemId, stepKey, "error", message, List.of(), null, null, null, null, null);
        }

        private static ProblemSubmitProgress error(String problemId, String stepKey, String message, List<String> detailLines) {
            // error 처리
            return new ProblemSubmitProgress(
                    problemId, stepKey, "error", message,
                    detailLines != null ? detailLines : List.of(),
                    null, null, null, null, null
            );
        }

        private static ProblemSubmitProgress runningStatement(String problemId, SubmittedStatement statement, String message) {
            // running 구문 처리
            return statement(problemId, statement, "running", message);
        }

        private static ProblemSubmitProgress successStatement(String problemId, SubmittedStatement statement, String message) {
            // success 구문 처리
            return statement(problemId, statement, "success", message);
        }

        private static ProblemSubmitProgress errorStatement(String problemId, SubmittedStatement statement, String message) {
            // error 구문 처리
            return statement(problemId, statement, "error", message);
        }

        private static ProblemSubmitProgress statement(String problemId, SubmittedStatement statement, String status, String message) {
            // statement 처리
            return new ProblemSubmitProgress(
                    problemId,
                    statement.key(),
                    status,
                    message,
                    List.of(),
                    statement.key(),
                    statement.index(),
                    statement.sql(),
                    statement.mode() == JudgeSqlExecutionMode.SELECT ? "select" : "command",
                    statement.referenceSelect()
            );
        }
    }

    private record SubmittedStatement(String key,
                                      int index,
                                      String sql,
                                      JudgeSqlExecutionMode mode,
                                      boolean referenceSelect) {
    }

    private record SubmittedExecutionContext(QueryExecutionResult referenceResult, String referenceSql) {
        // 제출 실행 Context 처리
    }

    private record SelectPageResult(List<String> columns, List<List<String>> rows) {
        // Select 페이지 Result 처리
    }

    private record PlanAnalysisResult(long executionPlanElement, List<String> summaryLines) {
        // 실행 계획 Analysis Result 처리
    }

    private static final class SubmittedDdlExecutionException extends RuntimeException {

        private final List<String> detailLines;

        private SubmittedDdlExecutionException(String message, List<String> detailLines, Throwable cause) {
            super(message, cause);
            this.detailLines = detailLines != null ? detailLines : List.of();
        }

        private List<String> detailLines() {
            // detail Lines 처리
            return detailLines;
        }
    }
}
