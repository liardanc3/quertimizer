package com.quertimizer.sqljudge.runtime;

import com.quertimizer.sqljudge.api.SqlJudge;
import com.quertimizer.sqljudge.command.CreateDatasetCommand;
import com.quertimizer.sqljudge.command.CreateEnvironmentCommand;
import com.quertimizer.sqljudge.command.CreateReferenceCommand;
import com.quertimizer.sqljudge.command.CreateSetupSqlCommand;
import com.quertimizer.sqljudge.command.ExecuteSqlCommand;
import com.quertimizer.sqljudge.command.IsolatedExecuteCommand;
import com.quertimizer.sqljudge.definition.DatasetDefinition;
import com.quertimizer.sqljudge.definition.InMemorySqlJudgeDefinitionStore;
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
import com.quertimizer.sqljudge.result.SqlExecutionResult;
import com.quertimizer.sqljudge.result.SqlReferenceResult;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 * Default sql-judge implementation placeholder for the later extracted module.
 */
public class DefaultSqlJudge implements SqlJudge {

    private final SqlJudgeDefinitionStore definitionStore;

    /**
     * Creates a default sql-judge placeholder with an in-memory definition store.
     */
    public DefaultSqlJudge() {
        this(new InMemorySqlJudgeDefinitionStore());
    }

    /**
     * Creates a default sql-judge placeholder.
     *
     * @param definitionStore sql-judge definition store
     */
    public DefaultSqlJudge(SqlJudgeDefinitionStore definitionStore) {
        this.definitionStore = Objects.requireNonNull(definitionStore, "definitionStore must not be null");
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
        requireDataset(command.getDatasetId());

        throw new UnsupportedOperationException("reference SQL registration is not implemented yet");
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

        throw new UnsupportedOperationException("create environment is not implemented yet");
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

        throw new UnsupportedOperationException("execute adapter mapping is not implemented yet");
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
        requireDataset(command.getDatasetId());
        command.getSetupSqlIds().forEach(setupSqlId -> requireSetupSql(command.getDatasetId(), setupSqlId));

        throw new UnsupportedOperationException("isolated execution is not implemented yet");
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

        throw new UnsupportedOperationException("drop environment is not implemented yet");
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
}
