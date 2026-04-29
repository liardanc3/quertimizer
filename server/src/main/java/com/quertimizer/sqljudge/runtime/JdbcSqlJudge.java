package com.quertimizer.sqljudge.runtime;

import com.quertimizer.sqljudge.api.SqlJudge;
import com.quertimizer.sqljudge.command.CreateDatasetCommand;
import com.quertimizer.sqljudge.command.CreateEnvironmentCommand;
import com.quertimizer.sqljudge.command.CreateReferenceCommand;
import com.quertimizer.sqljudge.command.CreateSetupSqlCommand;
import com.quertimizer.sqljudge.command.ExecuteSqlCommand;
import com.quertimizer.sqljudge.command.IsolatedExecuteCommand;
import com.quertimizer.sqljudge.db.SqlJudgeDialect;
import com.quertimizer.sqljudge.db.SqlJudgeDialectProvider;
import com.quertimizer.sqljudge.definition.DatasetDefinition;
import com.quertimizer.sqljudge.definition.ReferenceDefinition;
import com.quertimizer.sqljudge.definition.SetupSqlDefinition;
import com.quertimizer.sqljudge.definition.SqlJudgeDefinitionStore;
import com.quertimizer.sqljudge.event.ExecutionAccepted;
import com.quertimizer.sqljudge.event.ExecutionCompleted;
import com.quertimizer.sqljudge.event.ExecutionFailed;
import com.quertimizer.sqljudge.event.SqlJudgeListener;
import com.quertimizer.sqljudge.id.JudgeDatasetId;
import com.quertimizer.sqljudge.id.JudgeEnvironmentId;
import com.quertimizer.sqljudge.id.JudgeExecutionId;
import com.quertimizer.sqljudge.id.JudgeReferenceId;
import com.quertimizer.sqljudge.id.JudgeSetupSqlId;
import com.quertimizer.sqljudge.policy.SqlDefinitionPolicy;
import com.quertimizer.sqljudge.result.ExecutionMode;
import com.quertimizer.sqljudge.result.SqlExecutionResult;
import com.quertimizer.sqljudge.result.SqlReferenceResult;
import com.quertimizer.sqljudge.result.SqlResultHashSupport;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * JDBC-based sql-judge implementation for dataset registration and isolated execution.
 */
public class JdbcSqlJudge implements SqlJudge {

    private final RuntimeDatabaseCluster databaseCluster;
    private final SqlJudgeDefinitionStore definitionStore;
    private final SqlJudgeDialectProvider dialectProvider;
    private final RuntimeEnvironmentNamingStrategy namingStrategy;
    private final SqlStatementParser statementParser;
    private final SqlDefinitionPolicy definitionPolicy;

    /**
     * Creates a JDBC-based sql-judge implementation.
     *
     * @param databaseCluster runtime database cluster
     * @param definitionStore sql-judge definition store
     * @param dialectProvider DBMS dialect provider
     * @param namingStrategy runtime environment naming strategy
     * @param statementParser SQL statement parser
     * @param definitionPolicy SQL definition validation policy
     */
    public JdbcSqlJudge(RuntimeDatabaseCluster databaseCluster,
                        SqlJudgeDefinitionStore definitionStore,
                        SqlJudgeDialectProvider dialectProvider,
                        RuntimeEnvironmentNamingStrategy namingStrategy,
                        SqlStatementParser statementParser,
                        SqlDefinitionPolicy definitionPolicy) {
        this.databaseCluster = Objects.requireNonNull(databaseCluster, "databaseCluster must not be null");
        this.definitionStore = Objects.requireNonNull(definitionStore, "definitionStore must not be null");
        this.dialectProvider = Objects.requireNonNull(dialectProvider, "dialectProvider must not be null");
        this.namingStrategy = Objects.requireNonNull(namingStrategy, "namingStrategy must not be null");
        this.statementParser = Objects.requireNonNull(statementParser, "statementParser must not be null");
        this.definitionPolicy = Objects.requireNonNull(definitionPolicy, "definitionPolicy must not be null");
    }

    /**
     * Registers a reusable SQL dataset definition.
     *
     * @param command dataset registration command
     * @return registered dataset ID
     */
    @Override
    public JudgeDatasetId createDataset(CreateDatasetCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        definitionPolicy.validateDdl(command.getDdl());
        definitionPolicy.validateDataSql(command.getDataSql());

        JudgeDatasetId datasetId = new JudgeDatasetId("dataset-" + UUID.randomUUID());
        definitionStore.saveDataset(new DatasetDefinition(
                datasetId,
                command.getDbmsType(),
                command.getDdl(),
                command.getDataSql(),
                command.getBaseIndexDdls()
        ));

        return datasetId;
    }

    /**
     * Registers a reusable setup SQL bundle for a dataset.
     *
     * @param command setup SQL registration command
     * @return registered setup SQL bundle ID
     */
    @Override
    public JudgeSetupSqlId createSetupSql(CreateSetupSqlCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        requireDataset(command.getDatasetId());
        definitionPolicy.validateSetupSqls(command.getSetupSqls());

        JudgeSetupSqlId setupSqlId = new JudgeSetupSqlId("setup-" + UUID.randomUUID());
        definitionStore.saveSetupSql(new SetupSqlDefinition(
                setupSqlId,
                command.getDatasetId(),
                command.getSetupSqls(),
                command.getIndexPolicy()
        ));

        return setupSqlId;
    }

    /**
     * Registers a reusable reference SQL definition for a dataset.
     *
     * @param command reference SQL registration command
     * @return registered reference SQL result
     */
    @Override
    public SqlReferenceResult createReference(CreateReferenceCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        definitionPolicy.validateReadOnlySql(command.getReferenceSql());

        JudgeExecutionId executionId = new JudgeExecutionId("reference-" + UUID.randomUUID());
        SqlExecutionResult executionResult = executeIsolated(new IsolatedExecuteCommand(
                executionId,
                command.getDatasetId(),
                List.of(),
                command.getReferenceSql(),
                com.quertimizer.sqljudge.policy.IsolationPolicy.cleanRoom(),
                command.getOptions()
        ));
        String resultHash = SqlResultHashSupport.hashResult(executionResult.getColumns(), executionResult.getRows());
        JudgeReferenceId referenceId = new JudgeReferenceId("reference-" + UUID.randomUUID());
        definitionStore.saveReference(new ReferenceDefinition(
                referenceId,
                command.getDatasetId(),
                command.getReferenceSql(),
                resultHash
        ));

        return new SqlReferenceResult(referenceId, resultHash, executionResult);
    }

    /**
     * Creates a SQL execution environment from a registered dataset.
     *
     * @param command environment creation command
     * @return created environment ID
     */
    @Override
    public JudgeEnvironmentId create(CreateEnvironmentCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        requireDataset(command.getDatasetId());

        throw new UnsupportedOperationException("persistent environment creation is not implemented yet");
    }

    /**
     * Executes SQL asynchronously and emits execution events to the listener.
     *
     * @param command SQL execution command
     * @param listener sql-judge execution event listener
     * @return execution task ID
     */
    @Override
    public JudgeExecutionId executeAsync(ExecuteSqlCommand command, SqlJudgeListener listener) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(listener, "listener must not be null");

        listener.onEvent(new ExecutionAccepted(command.getExecutionId()));
        CompletableFuture.runAsync(() -> emitExecutionResult(command, listener));

        return command.getExecutionId();
    }

    /**
     * Executes SQL asynchronously and returns the completion result.
     *
     * @param command SQL execution command
     * @return SQL execution result future
     */
    @Override
    public CompletionStage<SqlExecutionResult> executeAsync(ExecuteSqlCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return CompletableFuture.supplyAsync(() -> execute(command));
    }

    /**
     * Executes SQL synchronously.
     *
     * @param command SQL execution command
     * @return SQL execution result
     */
    @Override
    public SqlExecutionResult execute(ExecuteSqlCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        throw new UnsupportedOperationException("persistent environment execution is not implemented yet");
    }

    /**
     * Executes SQL asynchronously in a clean isolated environment and emits execution events to the listener.
     *
     * @param command isolated execution command
     * @param listener sql-judge execution event listener
     * @return execution task ID
     */
    @Override
    public JudgeExecutionId executeIsolatedAsync(IsolatedExecuteCommand command, SqlJudgeListener listener) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(listener, "listener must not be null");

        listener.onEvent(new ExecutionAccepted(command.getExecutionId()));
        CompletableFuture.runAsync(() -> emitIsolatedExecutionResult(command, listener));

        return command.getExecutionId();
    }

    /**
     * Executes SQL asynchronously in a clean isolated environment and returns the completion result.
     *
     * @param command isolated execution command
     * @return SQL execution result future
     */
    @Override
    public CompletionStage<SqlExecutionResult> executeIsolatedAsync(IsolatedExecuteCommand command) {
        Objects.requireNonNull(command, "command must not be null");

        return CompletableFuture.supplyAsync(() -> executeIsolated(command));
    }

    /**
     * Executes SQL synchronously in a clean isolated environment.
     *
     * @param command isolated execution command
     * @return SQL execution result
     */
    @Override
    public SqlExecutionResult executeIsolated(IsolatedExecuteCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        definitionPolicy.validateReadOnlySql(command.getTargetSql());

        DatasetDefinition dataset = requireDataset(command.getDatasetId());
        List<SetupSqlDefinition> setupSqlDefinitions = command.getSetupSqlIds().stream()
                .map(setupSqlId -> requireSetupSql(command.getDatasetId(), setupSqlId))
                .toList();

        return executeInTemporaryEnvironment(command, dataset, setupSqlDefinitions);
    }

    /**
     * Cancels a running SQL execution task.
     *
     * @param executionId execution task ID
     */
    @Override
    public void cancel(JudgeExecutionId executionId) {
        Objects.requireNonNull(executionId, "executionId must not be null");

        throw new UnsupportedOperationException("cancel execution is not implemented yet");
    }

    /**
     * Drops a SQL execution environment.
     *
     * @param environmentId execution environment ID
     */
    @Override
    public void drop(JudgeEnvironmentId environmentId) {
        Objects.requireNonNull(environmentId, "environmentId must not be null");

        throw new UnsupportedOperationException("persistent environment drop is not implemented yet");
    }

    private SqlExecutionResult executeInTemporaryEnvironment(IsolatedExecuteCommand command,
                                                            DatasetDefinition dataset,
                                                            List<SetupSqlDefinition> setupSqlDefinitions) {
        try (RuntimeDatabaseLease lease = databaseCluster.acquire(dataset.getDbmsType());
             Connection connection = lease.openConnection()) {
            JudgeEnvironmentId environmentId = new JudgeEnvironmentId("environment-" + UUID.randomUUID());
            RuntimeEnvironmentName environmentName = namingStrategy.createName(environmentId, dataset.getDatasetId());
            SqlJudgeDialect dialect = dialectProvider.get(dataset.getDbmsType());
            RuntimeEnvironment environment = new RuntimeEnvironment(
                    environmentId,
                    dataset.getDatasetId(),
                    lease.getDatabase(),
                    environmentName,
                    Instant.now()
            );

            return executeWithEnvironment(command, dataset, setupSqlDefinitions, connection, dialect, environment);
        } catch (Exception exception) {
            throw new IllegalStateException("sql-judge isolated execution failed", exception);
        }
    }

    private SqlExecutionResult executeWithEnvironment(IsolatedExecuteCommand command,
                                                      DatasetDefinition dataset,
                                                      List<SetupSqlDefinition> setupSqlDefinitions,
                                                      Connection connection,
                                                      SqlJudgeDialect dialect,
                                                      RuntimeEnvironment environment) throws Exception {
        String environmentName = environment.getName().getValue();
        connection.setAutoCommit(false);
        try {
            createEnvironment(connection, dialect, environmentName);
            executeStatements(connection, dataset.getDdl());
            executeStatements(connection, dataset.getDataSql());
            for (String baseIndexDdl : dataset.getBaseIndexDdls()) {
                executeStatements(connection, baseIndexDdl);
            }
            for (SetupSqlDefinition setupSqlDefinition : setupSqlDefinitions) {
                for (String setupSql : setupSqlDefinition.getSetupSqls()) {
                    executeStatements(connection, setupSql);
                }
            }
            SqlExecutionResult result = executeSelect(connection, dialect, environmentName, command.getTargetSql(), command.getOptions().getTimeoutSeconds());
            connection.commit();
            return result;
        } catch (Exception exception) {
            rollback(connection);
            throw exception;
        } finally {
            cleanupEnvironment(connection, dialect, environmentName);
        }
    }

    private void createEnvironment(Connection connection, SqlJudgeDialect dialect, String environmentName) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.execute(dialect.dropEnvironmentIfExistsSql(environmentName));
            statement.execute(dialect.createEnvironmentSql(environmentName));
            for (String useEnvironmentSql : dialect.useEnvironmentSqls(environmentName)) {
                statement.execute(useEnvironmentSql);
            }
        }
    }

    private void executeStatements(Connection connection, String sql) throws Exception {
        for (String statementSql : statementParser.splitStatements(sql)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute(statementSql);
            }
        }
    }

    private SqlExecutionResult executeSelect(Connection connection,
                                            SqlJudgeDialect dialect,
                                            String environmentName,
                                            String sql,
                                            int timeoutSeconds) throws Exception {
        try (Statement statement = connection.createStatement()) {
            for (String useEnvironmentSql : dialect.useEnvironmentSqls(environmentName)) {
                statement.execute(useEnvironmentSql);
            }
            for (String timeoutSql : dialect.statementTimeoutSqls(timeoutSeconds)) {
                statement.execute(timeoutSql);
            }
            statement.execute(sql);

            try (ResultSet resultSet = statement.getResultSet()) {
                if (resultSet == null) {
                    throw new IllegalArgumentException("SQL did not return a result set");
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

                return new SqlExecutionResult(
                        ExecutionMode.SELECT,
                        columns,
                        rows,
                        rows.size(),
                        1,
                        rows.size(),
                        null,
                        null,
                        List.of(),
                        "SQL execution completed"
                );
            }
        }
    }

    private void cleanupEnvironment(Connection connection, SqlJudgeDialect dialect, String environmentName) {
        try (Statement statement = connection.createStatement()) {
            connection.setAutoCommit(true);
            statement.execute(dialect.dropEnvironmentIfExistsSql(environmentName));
        } catch (Exception ignored) {
        }
    }

    private void rollback(Connection connection) {
        try {
            connection.rollback();
        } catch (Exception ignored) {
        }
    }

    private DatasetDefinition requireDataset(JudgeDatasetId datasetId) {
        return definitionStore.findDataset(datasetId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown dataset ID: " + datasetId));
    }

    private SetupSqlDefinition requireSetupSql(JudgeDatasetId datasetId, JudgeSetupSqlId setupSqlId) {
        SetupSqlDefinition setupSqlDefinition = definitionStore.findSetupSql(setupSqlId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown setup SQL bundle ID: " + setupSqlId));
        if (!setupSqlDefinition.getDatasetId().equals(datasetId)) {
            throw new IllegalArgumentException("Setup SQL bundle targets a different dataset: " + setupSqlId);
        }

        return setupSqlDefinition;
    }

    private void emitExecutionResult(ExecuteSqlCommand command, SqlJudgeListener listener) {
        try {
            SqlExecutionResult result = execute(command);
            listener.onEvent(new ExecutionCompleted(command.getExecutionId(), result));
        } catch (Exception exception) {
            listener.onEvent(new ExecutionFailed(command.getExecutionId(), exception.getMessage(), exception));
        }
    }

    private void emitIsolatedExecutionResult(IsolatedExecuteCommand command, SqlJudgeListener listener) {
        try {
            SqlExecutionResult result = executeIsolated(command);
            listener.onEvent(new ExecutionCompleted(command.getExecutionId(), result));
        } catch (Exception exception) {
            listener.onEvent(new ExecutionFailed(command.getExecutionId(), exception.getMessage(), exception));
        }
    }
}
