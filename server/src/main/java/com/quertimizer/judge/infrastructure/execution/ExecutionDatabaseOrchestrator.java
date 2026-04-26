package com.quertimizer.judge.infrastructure.execution;

import com.quertimizer.global.constant.DbmsType;
import com.quertimizer.judge.application.input.GenerateAnswerHashInput;
import com.quertimizer.judge.application.input.ProblemOutputPreviewInput;
import com.quertimizer.judge.application.output.ProblemOutputPreviewOutput;
import com.quertimizer.judge.application.port.JudgeExecutionOrchestratorPort;
import com.quertimizer.judge.infrastructure.config.JudgeDatabaseProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ExecutionDatabaseOrchestrator implements JudgeExecutionOrchestratorPort {

    private final JudgeDatabaseProperties judgeDatabaseProperties;
    private final ExecutionDatabaseQueue executionDatabaseQueue;
    private final SqlReplayProvisioningStrategy sqlReplayProvisioningStrategy;
    private final TemplateSchemaCopyProvisioningStrategy templateSchemaCopyProvisioningStrategy;

    @Override
    public ProblemOutputPreviewOutput executeProblemOutputPreview(ProblemOutputPreviewInput input) {
        // 문제 생성 화면의 출력 예시 preview를 임시 execution schema에서 생성
        return executePreviewQuery(input.dbmsType(), input.ddl(), input.sampleDataSql(), input.answerSql(), "preview");
    }

    @Override
    public ProblemOutputPreviewOutput executeAnswerHashSource(GenerateAnswerHashInput input) {
        // 실제 채점 데이터셋의 answerSql 결과를 임시 execution schema에서 생성
        return executePreviewQuery(input.dbmsType(), input.ddl(), input.actualDataSql(), input.answerSql(), "hash");
    }

    private ProblemOutputPreviewOutput executePreviewQuery(DbmsType dbmsType,
                                                           String ddl,
                                                           String dataSql,
                                                           String answerSql,
                                                           String purpose) {
        if (dbmsType == DbmsType.ORACLE) {
            throw new IllegalStateException("Oracle execution orchestrator는 아직 지원하지 않는다.");
        }

        ExecutionDatabasePool.ExecutionDatabaseWorker worker = executionDatabaseQueue.acquire(dbmsType);
        String schemaName = createExecutionSchemaName(purpose);

        try (Connection connection = DriverManager.getConnection(
                worker.getConnectionInfo().url(),
                worker.getConnectionInfo().username(),
                worker.getConnectionInfo().password())) {
            connection.setAutoCommit(false);
            provisionDataset(connection, dbmsType, schemaName, ddl, dataSql);
            ProblemOutputPreviewOutput output = executeSelect(connection, schemaName, answerSql);
            connection.commit();
            return output;
        } catch (Exception exception) {
            throw new IllegalStateException("judge execution 환경 처리에 실패했다.", exception);
        } finally {
            cleanupSchema(worker.getConnectionInfo(), schemaName);
            executionDatabaseQueue.release(worker);
        }
    }

    private void provisionDataset(Connection connection, DbmsType dbmsType, String schemaName, String ddl, String dataSql) throws Exception {
        // properties 전략에 따라 execution schema dataset을 준비
        String strategy = judgeDatabaseProperties.getProvisioningStrategy();
        if ("sql-replay".equalsIgnoreCase(strategy)) {
            sqlReplayProvisioningStrategy.provision(connection, schemaName, ddl, dataSql);
            return;
        }

        if ("template-copy".equalsIgnoreCase(strategy)) {
            validateTemplateCopyConfiguration(dbmsType);
            templateSchemaCopyProvisioningStrategy.provision(connection, schemaName, ddl, dataSql);
            return;
        }

        throw new IllegalStateException("지원하지 않는 judge.dataset-provisioning.strategy 값이다: " + strategy);
    }

    private void validateTemplateCopyConfiguration(DbmsType dbmsType) {
        // template-copy 전략은 template DB 설정이 선행되어야 한다
        JudgeDatabaseProperties.NamedDatabaseProperties properties = judgeDatabaseProperties.getTemplateDatabase(dbmsType);
        if (properties == null || isBlank(properties.getUrl()) || isBlank(properties.getUsername())) {
            throw new IllegalStateException("template-copy 전략에는 judge.template-databases.%s 설정이 필요하다.".formatted(dbmsType.getValue()));
        }
    }

    private ProblemOutputPreviewOutput executeSelect(Connection connection, String schemaName, String sql) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(60);
            statement.execute("SET LOCAL search_path TO " + quoteIdentifier(schemaName) + ", public");
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
                while (resultSet.next()) {
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

    private void cleanupSchema(ExecutionDatabaseConnectionInfo connectionInfo, String schemaName) {
        // 임시 execution schema만 정리하고 template schema는 남겨 둔다
        try (Connection connection = DriverManager.getConnection(connectionInfo.url(), connectionInfo.username(), connectionInfo.password());
             Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + quoteIdentifier(schemaName) + " CASCADE");
        } catch (Exception ignored) {
        }
    }

    private String createExecutionSchemaName(String purpose) {
        return "judge_exec_" + purpose + "_" + Long.toString(System.nanoTime(), 36);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String quoteIdentifier(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }
}
