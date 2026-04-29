package com.quertimizer.judge.infrastructure.execution;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.judge.application.input.ProblemOutputPreviewInput;
import com.quertimizer.judge.application.output.ProblemOutputPreviewOutput;
import com.quertimizer.judge.application.port.DbmsSqlDialect;
import com.quertimizer.judge.application.port.JudgeExecutionOrchestratorPort;
import com.quertimizer.judge.infrastructure.config.JudgeDatabaseProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ExecutionDatabaseOrchestrator implements JudgeExecutionOrchestratorPort {

    private static final int MAX_RESULT_ROWS = 100;
    private static final int MAX_RESULT_COLUMNS = 50;

    private final JudgeDatabaseProperties judgeDatabaseProperties;
    private final ExecutionDatabaseQueue executionDatabaseQueue;
    private final SqlReplayProvisioningStrategy sqlReplayProvisioningStrategy;
    private final TemplateSchemaCopyProvisioningStrategy templateSchemaCopyProvisioningStrategy;
    private final DbmsSqlDialects dbmsSqlDialects;

    @Override
    public ProblemOutputPreviewOutput executeProblemOutputPreview(ProblemOutputPreviewInput input) {
        // 문제 생성 화면의 출력 예시 preview를 임시 execution schema에서 생성
        return executePreviewQuery(input.dbmsType(), input.ddl(), input.sampleDataSql(), input.answerSql(), "preview");
    }

    private ProblemOutputPreviewOutput executePreviewQuery(DbmsType dbmsType,
                                                           String ddl,
                                                           String dataSql,
                                                           String answerSql,
                                                           String purpose) {
        String schemaName = createExecutionSchemaName(purpose);
        JudgeDatabaseLease lease = executionDatabaseQueue.acquire(dbmsType);

        try (lease; Connection connection = lease.openConnection()) {
            connection.setAutoCommit(false);
            try {
                provisionDataset(connection, dbmsType, schemaName, ddl, dataSql);
                ProblemOutputPreviewOutput output = executeSelect(connection, dbmsType, schemaName, answerSql);
                connection.commit();
                return output;
            } catch (Exception exception) {
                rollback(connection);
                throw exception;
            } finally {
                cleanupSchema(connection, dbmsType, schemaName);
            }
        } catch (Exception exception) {
            throw new IllegalStateException("judge execution 환경 처리에 실패했다.", exception);
        }
    }

    private void provisionDataset(Connection connection, DbmsType dbmsType, String schemaName, String ddl, String dataSql) throws Exception {
        // properties 전략에 따라 execution schema dataset을 준비
        String strategy = judgeDatabaseProperties.getProvisioningStrategy();
        if ("sql-replay".equalsIgnoreCase(strategy)) {
            sqlReplayProvisioningStrategy.provision(connection, dbmsType, schemaName, ddl, dataSql);
            return;
        }

        if ("template-copy".equalsIgnoreCase(strategy)) {
            templateSchemaCopyProvisioningStrategy.provision(connection, dbmsType, schemaName, ddl, dataSql);
            return;
        }

        throw new IllegalStateException("지원하지 않는 judge.dataset-provisioning.strategy 값이다: " + strategy);
    }

    private ProblemOutputPreviewOutput executeSelect(Connection connection, DbmsType dbmsType, String schemaName, String sql) throws Exception {
        DbmsSqlDialect dialect = dbmsSqlDialects.get(dbmsType);
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(60);
            for (String useSchemaSql : dialect.useSchemaSqls(schemaName)) {
                statement.execute(useSchemaSql);
            }
            statement.setMaxRows(MAX_RESULT_ROWS + 1);
            statement.execute(sql);

            try (ResultSet resultSet = statement.getResultSet()) {
                if (resultSet == null) {
                    throw new IllegalArgumentException("SELECT 결과를 확인할 수 없다.");
                }

                ResultSetMetaData metaData = resultSet.getMetaData();
                if (metaData.getColumnCount() > MAX_RESULT_COLUMNS) {
                    throw new IllegalArgumentException("SQL 실행 결과 컬럼 수가 너무 많습니다.");
                }

                List<String> columns = new ArrayList<>();
                for (int columnIndex = 1; columnIndex <= metaData.getColumnCount(); columnIndex++) {
                    columns.add(metaData.getColumnLabel(columnIndex));
                }

                List<List<String>> rows = new ArrayList<>();
                while (resultSet.next()) {
                    if (rows.size() >= MAX_RESULT_ROWS) {
                        throw new IllegalArgumentException("SQL 실행 결과 행 수가 너무 많습니다.");
                    }

                    List<String> row = new ArrayList<>();
                    for (int columnIndex = 1; columnIndex <= metaData.getColumnCount(); columnIndex++) {
                        Object value = resultSet.getObject(columnIndex);
                        row.add(value != null ? String.valueOf(value) : null);
                    }
                    rows.add(row);
                }

                return new ProblemOutputPreviewOutput(columns, rows, rows.size());
            }
        }
    }

    private void cleanupSchema(Connection connection, DbmsType dbmsType, String schemaName) {
        // 임시 execution schema만 정리하고 template schema는 남겨 둔다
        try (Statement statement = connection.createStatement()) {
            connection.setAutoCommit(true);
            statement.execute(dbmsSqlDialects.get(dbmsType).dropSchemaIfExistsSql(schemaName));
        } catch (Exception ignored) {
        }
    }

    private void rollback(Connection connection) {
        // 실패한 execution transaction을 정리
        try {
            connection.rollback();
        } catch (Exception ignored) {
        }
    }

    private String createExecutionSchemaName(String purpose) {
        return "qt_exec_" + purpose + "_" + Long.toString(System.nanoTime(), 36);
    }

}
