package com.quertimizer.judge.adapter.out.jdbc;

import com.quertimizer.judge.application.input.ExecuteSqlInput;
import com.quertimizer.judge.application.model.EnvironmentConnection;
import com.quertimizer.judge.application.output.SqlExecutionResult;
import com.quertimizer.judge.application.port.out.SqlExecutionPort;
import com.quertimizer.judge.domain.entity.JudgeExecutionId;
import com.quertimizer.judge.domain.entity.SetupSqlDefinition;
import com.quertimizer.judge.domain.model.ExecutionMode;
import com.quertimizer.judge.application.port.out.SqlDialect;
import com.quertimizer.judge.adapter.out.jdbc.dialect.SqlPlanCostParser;
import com.quertimizer.judge.domain.service.SqlStatementParser;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static com.quertimizer.judge.domain.model.JudgeFailReason.ANALYZE_API_REQUIRED;
import static com.quertimizer.judge.domain.model.JudgeFailReason.SQL_PLAN_NOT_RETURNED;
import static com.quertimizer.judge.domain.model.JudgeFailReason.SQL_RESULT_SET_NOT_RETURNED;
import static com.quertimizer.judge.domain.model.SqlPolicyFailReason.SINGLE_SQL_ONLY;

@Component
@RequiredArgsConstructor
public class JdbcExecutor implements SqlExecutionPort {

    private final SqlStatementParser statementParser;
    private final JdbcEnvironment environment;
    private final ConcurrentHashMap<JudgeExecutionId, Statement> activeStatements = new ConcurrentHashMap<>();

    @Override
    public SqlExecutionResult execute(ExecuteSqlInput command, String sql, ExecutionMode mode,
                                      EnvironmentConnection environmentConnection) throws Exception {
        // 실행 환경 연결에서 JDBC connection과 dialect 추출
        return execute(command, sql, mode, environmentConnection.getConnection(), environmentConnection.getDialect());
    }

    @Override
    public void executeSetupSqls(EnvironmentConnection environmentConnection,
                                 List<SetupSqlDefinition> setupSqlDefinitions) throws Exception {
        // 등록된 설정 SQL 순차 실행
        executeSetupSqls(environmentConnection.getConnection(), setupSqlDefinitions);
    }

    @Override
    public SqlExecutionResult executeSelectAll(JudgeExecutionId executionId,
                                               EnvironmentConnection environmentConnection,
                                               String sql, boolean includeCost,
                                               boolean includePlan) throws Exception {
        // 실행 환경 연결 기준 SELECT 전체 결과 조회
        return executeSelectAll(
                executionId, environmentConnection.getConnection(), environmentConnection.getDialect(),
                sql, includeCost, includePlan
        );
    }

    @Override
    public SqlExecutionResult executeAnalyze(JudgeExecutionId executionId,
                                             EnvironmentConnection environmentConnection) throws Exception {
        // 실행 환경 연결 기준 DBMS 통계 갱신
        return executeAnalyze(
                executionId, environmentConnection.getConnection(),
                environmentConnection.getDialect(), environmentConnection.getEnvironmentName()
        );
    }

    private SqlExecutionResult execute(ExecuteSqlInput command, String sql, ExecutionMode mode,
                                       Connection connection, SqlDialect dialect) throws Exception {
        // SQL 실행 모드별 처리 흐름 분기
        return switch (mode) {
            case SELECT -> executeSelectPage(
                    command.getExecutionId(), connection, dialect, sql,
                    command.getOptions().getPage(), command.getOptions().getPageSize(),
                    command.getOptions().isIncludeCost(), command.getOptions().isIncludePlan()
            );
            case EXPLAIN, EXPLAIN_ANALYZE -> executePlan(command.getExecutionId(), connection, dialect, sql, mode);
            case ANALYZE -> throw new IllegalArgumentException(ANALYZE_API_REQUIRED.getMessage());
            case INDEX_COMMAND -> executeCommand(command.getExecutionId(), connection, sql, mode);
            case COMMAND -> executeCommand(command.getExecutionId(), connection, sql, mode);
        };
    }

    private void executeSetupSqls(Connection connection, List<SetupSqlDefinition> setupSqlDefinitions) throws Exception {
        // 등록된 설정 SQL 순차 실행
        for (SetupSqlDefinition setupSqlDefinition : setupSqlDefinitions) {
            for (String setupSql : setupSqlDefinition.getSetupSqls()) {
                executeStatements(connection, setupSql);
            }
        }
    }

    private SqlExecutionResult executeSelectAll(JudgeExecutionId executionId, Connection connection,
                                                SqlDialect dialect, String sql,
                                                boolean includeCost,
                                                boolean includePlan) throws Exception {
        // 단일 SELECT 문장 정리와 전체 결과 조회
        String statementSql = resolveSingleStatement(sql);
        long startTime = System.nanoTime();
        List<String> planLines = includeCost || includePlan
                ? executePlanLines(executionId, connection, dialect.explainSql(statementSql))
                : List.of();
        BigDecimal cost = includeCost ? SqlPlanCostParser.extractEstimatedCost(planLines) : null;
        SqlTableResult tableResult = fetchSelectAll(executionId, connection, statementSql);

        return new SqlExecutionResult(
                ExecutionMode.SELECT, tableResult.columns, tableResult.rows,
                tableResult.rows.size(), 1, Math.max(tableResult.rows.size(), 1),
                Duration.ofNanos(System.nanoTime() - startTime).toMillis(), cost,
                includePlan ? planLines : List.of(), "SQL 실행 완료"
        );
    }

    private SqlExecutionResult executeAnalyze(JudgeExecutionId executionId, Connection connection,
                                              SqlDialect dialect, String environmentName) throws Exception {
        // DBMS 통계 갱신 실행과 결과 생성
        long startTime = System.nanoTime();
        environment.initializeStatistics(executionId, activeStatements, connection, dialect, environmentName);

        return new SqlExecutionResult(
                ExecutionMode.ANALYZE, List.of(), List.of(),
                0, 1, 1,
                Duration.ofNanos(System.nanoTime() - startTime).toMillis(), null,
                List.of(), "SQL 통계 갱신 완료"
        );
    }

    @Override
    public void cancel(JudgeExecutionId executionId) {
        // 취소 대상 실행 ID 기준 추적 문장 확인
        Statement activeStatement = activeStatements.remove(executionId);
        if (activeStatement == null) {
            return;
        }

        // 실행 중 문장 취소와 리소스 정리
        try {
            activeStatement.cancel();
        } catch (Exception ignored) {
        }

        try {
            activeStatement.close();
        } catch (Exception ignored) {
        }
    }

    @Override
    public boolean hasActiveExecution(JudgeExecutionId executionId) {
        // 실행 ID 기준 추적 Statement 존재 여부 확인
        return activeStatements.containsKey(executionId);
    }

    public String resolveSingleStatement(String sql) {
        // 단일 SQL 문장 분리
        List<String> statements = statementParser.splitStatements(sql);
        if (statements.size() != 1) {
            throw new IllegalArgumentException(SINGLE_SQL_ONLY.getMessage());
        }

        return statements.get(0);
    }

    private void executeStatements(Connection connection, String sql) throws Exception {
        // SQL 문자열을 실행 가능한 문장으로 분리해 순차 실행
        for (String statementSql : statementParser.splitStatements(sql)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(statementSql);
            }
        }
    }

    private SqlExecutionResult executeSelectPage(JudgeExecutionId executionId, Connection connection,
                                                 SqlDialect dialect, String sql,
                                                 int page, int pageSize,
                                                 boolean includeCost,
                                                 boolean includePlan) throws Exception {
        // 실행 계획과 비용, 전체 행 수, 현재 페이지 결과 조회
        long startTime = System.nanoTime();
        List<String> planLines = includeCost || includePlan
                ? executePlanLines(executionId, connection, dialect.explainSql(sql))
                : List.of();
        BigDecimal cost = includeCost ? SqlPlanCostParser.extractEstimatedCost(planLines) : null;
        long rowCount = fetchSelectRowCount(executionId, connection, dialect, sql);
        int totalPages = Math.max(1, (int) Math.ceil((double) rowCount / pageSize));
        int currentPage = Math.min(page, totalPages);
        SqlTableResult pageResult = fetchSelectPage(executionId, connection, dialect, sql, currentPage, pageSize);

        return new SqlExecutionResult(
                ExecutionMode.SELECT, pageResult.columns, pageResult.rows,
                rowCount, currentPage, pageSize,
                Duration.ofNanos(System.nanoTime() - startTime).toMillis(), cost,
                includePlan ? planLines : List.of(), "SQL 실행 완료"
        );
    }

    private SqlExecutionResult executePlan(JudgeExecutionId executionId, Connection connection,
                                           SqlDialect dialect, String sql,
                                           ExecutionMode mode) throws Exception {
        // 실행 계획 SQL 실행과 비용 추출
        long startTime = System.nanoTime();
        List<String> planLines = executePlanLines(executionId, connection, dialect.planSql(sql, mode));

        return new SqlExecutionResult(
                mode, List.of(), List.of(),
                planLines.size(), 1, Math.max(planLines.size(), 1),
                Duration.ofNanos(System.nanoTime() - startTime).toMillis(),
                SqlPlanCostParser.extractEstimatedCost(planLines),
                planLines, "SQL 실행 계획 반환"
        );
    }

    private SqlExecutionResult executeCommand(JudgeExecutionId executionId, Connection connection,
                                              String sql, ExecutionMode mode) throws Exception {
        // DDL 또는 명령 SQL 실행과 변경 행 수 반환
        long startTime = System.nanoTime();
        Statement statement = createTrackedStatement(executionId, connection);
        try (statement) {
            statement.execute(sql);
            int updateCount = Math.max(statement.getUpdateCount(), 0);

            return new SqlExecutionResult(
                    mode, List.of(), List.of(),
                    updateCount, 1, 1,
                    Duration.ofNanos(System.nanoTime() - startTime).toMillis(), null,
                    List.of(), "SQL 명령 실행 완료"
            );
        } finally {
            clearTrackedStatement(executionId, statement);
        }
    }

    private long fetchSelectRowCount(JudgeExecutionId executionId, Connection connection,
                                     SqlDialect dialect, String sql) throws Exception {
        // SELECT 전체 행 수 조회
        Statement statement = createTrackedStatement(executionId, connection);
        try (statement) {
            statement.execute(dialect.selectCountSql(sql));

            try (ResultSet resultSet = statement.getResultSet()) {
                if (resultSet == null || !resultSet.next()) {
                    return 0;
                }

                return resultSet.getLong(1);
            }
        } finally {
            clearTrackedStatement(executionId, statement);
        }
    }

    private SqlTableResult fetchSelectPage(JudgeExecutionId executionId, Connection connection,
                                           SqlDialect dialect, String sql,
                                           int page, int pageSize) throws Exception {
        // SELECT 페이지 결과 조회
        PreparedStatement statement = createTrackedPreparedStatement(executionId, connection, dialect.selectPageSql(sql));
        try (statement) {
            statement.setInt(1, pageSize);
            statement.setLong(2, (long) (page - 1) * pageSize);
            statement.execute();

            try (ResultSet resultSet = statement.getResultSet()) {
                if (resultSet == null) {
                    throw new IllegalArgumentException(SQL_RESULT_SET_NOT_RETURNED.getMessage());
                }

                return readTableResult(resultSet);
            }
        } finally {
            clearTrackedStatement(executionId, statement);
        }
    }

    private SqlTableResult fetchSelectAll(JudgeExecutionId executionId, Connection connection, String sql) throws Exception {
        // SELECT 전체 결과 조회
        Statement statement = createTrackedStatement(executionId, connection);
        try (statement) {
            statement.execute(sql);

            try (ResultSet resultSet = statement.getResultSet()) {
                if (resultSet == null) {
                    throw new IllegalArgumentException(SQL_RESULT_SET_NOT_RETURNED.getMessage());
                }

                return readTableResult(resultSet);
            }
        } finally {
            clearTrackedStatement(executionId, statement);
        }
    }

    private List<String> executePlanLines(JudgeExecutionId executionId, Connection connection, String sql) throws Exception {
        // 실행 계획 결과 라인 조회
        Statement statement = createTrackedStatement(executionId, connection);
        try (statement) {
            statement.execute(sql);

            try (ResultSet resultSet = statement.getResultSet()) {
                if (resultSet == null) {
                    throw new IllegalArgumentException(SQL_PLAN_NOT_RETURNED.getMessage());
                }

                return readPlanLines(resultSet);
            }
        } finally {
            clearTrackedStatement(executionId, statement);
        }
    }

    private SqlTableResult readTableResult(ResultSet resultSet) throws Exception {
        // 결과 집합 메타데이터 기준 컬럼명 추출
        ResultSetMetaData metaData = resultSet.getMetaData();
        List<String> columns = new ArrayList<>();
        for (int columnIndex = 1; columnIndex <= metaData.getColumnCount(); columnIndex++) {
            columns.add(metaData.getColumnLabel(columnIndex));
        }

        // 결과 집합 행 데이터 문자열 변환
        List<List<String>> rows = new ArrayList<>();
        while (resultSet.next()) {
            List<String> row = new ArrayList<>();
            for (int columnIndex = 1; columnIndex <= metaData.getColumnCount(); columnIndex++) {
                Object value = resultSet.getObject(columnIndex);
                row.add(value != null ? String.valueOf(value) : "null");
            }
            rows.add(row);
        }

        return new SqlTableResult(columns, rows);
    }

    private List<String> readPlanLines(ResultSet resultSet) throws Exception {
        // 실행 계획 결과 집합을 문자열 라인으로 변환
        ResultSetMetaData metaData = resultSet.getMetaData();
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

    private Statement createTrackedStatement(JudgeExecutionId executionId, Connection connection) throws Exception {
        // 실행 취소용 Statement 추적 등록
        Statement statement = connection.createStatement();
        if (executionId != null) {
            activeStatements.put(executionId, statement);
        }
        return statement;
    }

    private PreparedStatement createTrackedPreparedStatement(JudgeExecutionId executionId, Connection connection,
                                                            String sql) throws Exception {
        // 실행 취소용 PreparedStatement 추적 등록
        PreparedStatement statement = connection.prepareStatement(sql);
        if (executionId != null) {
            activeStatements.put(executionId, statement);
        }
        return statement;
    }

    private void clearTrackedStatement(JudgeExecutionId executionId, Statement statement) {
        // 실행 취소 추적 Statement 제거
        if (executionId != null) {
            activeStatements.remove(executionId, statement);
        }
    }

    @Data
    private static final class SqlTableResult {
        private final List<String> columns;
        private final List<List<String>> rows;

        private SqlTableResult(List<String> columns, List<List<String>> rows) {
            this.columns = List.copyOf(columns);
            this.rows = rows.stream().map(List::copyOf).toList();
        }
    }
}
