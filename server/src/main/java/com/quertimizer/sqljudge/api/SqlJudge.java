package com.quertimizer.sqljudge.api;

import com.quertimizer.sqljudge.command.CreateDatasetCommand;
import com.quertimizer.sqljudge.command.CreateEnvironmentCommand;
import com.quertimizer.sqljudge.command.CreateReferenceCommand;
import com.quertimizer.sqljudge.command.CreateSetupSqlCommand;
import com.quertimizer.sqljudge.command.ExecuteSqlCommand;
import com.quertimizer.sqljudge.command.IsolatedExecuteCommand;
import com.quertimizer.sqljudge.event.SqlJudgeListener;
import com.quertimizer.sqljudge.id.JudgeDatasetId;
import com.quertimizer.sqljudge.id.JudgeEnvironmentId;
import com.quertimizer.sqljudge.id.JudgeExecutionId;
import com.quertimizer.sqljudge.id.JudgeSetupSqlId;
import com.quertimizer.sqljudge.result.SqlReferenceResult;
import com.quertimizer.sqljudge.result.SqlExecutionResult;

import java.util.concurrent.CompletionStage;

/**
 * Provides sql-judge environment creation, execution, isolated execution, cancellation, and cleanup.
 */
public interface SqlJudge {

    /**
     * Registers a reusable SQL dataset definition.
     *
     * @param command dataset registration command
     * @return registered dataset ID
     */
    JudgeDatasetId createDataset(CreateDatasetCommand command);

    /**
     * Registers a reusable setup SQL bundle for a dataset.
     *
     * @param command setup SQL registration command
     * @return registered setup SQL bundle ID
     */
    JudgeSetupSqlId createSetupSql(CreateSetupSqlCommand command);

    /**
     * Registers a reusable reference SQL definition for a dataset.
     *
     * @param command reference SQL registration command
     * @return registered reference SQL result
     */
    SqlReferenceResult createReference(CreateReferenceCommand command);

    /**
     * Creates a SQL execution environment from a registered dataset.
     *
     * @param command environment creation command
     * @return created environment ID
     */
    JudgeEnvironmentId create(CreateEnvironmentCommand command);

    /**
     * Executes SQL asynchronously and emits execution events to the listener.
     *
     * @param command SQL execution command
     * @param listener sql-judge execution event listener
     * @return execution task ID
     */
    JudgeExecutionId executeAsync(ExecuteSqlCommand command, SqlJudgeListener listener);

    /**
     * Executes SQL asynchronously and returns the completion result.
     *
     * @param command SQL execution command
     * @return SQL execution result future
     */
    CompletionStage<SqlExecutionResult> executeAsync(ExecuteSqlCommand command);

    /**
     * Executes SQL synchronously.
     *
     * @param command SQL execution command
     * @return SQL execution result
     */
    SqlExecutionResult execute(ExecuteSqlCommand command);

    /**
     * Executes SQL asynchronously in a clean isolated environment and emits execution events to the listener.
     *
     * @param command isolated execution command
     * @param listener sql-judge execution event listener
     * @return execution task ID
     */
    JudgeExecutionId executeIsolatedAsync(IsolatedExecuteCommand command, SqlJudgeListener listener);

    /**
     * Executes SQL asynchronously in a clean isolated environment and returns the completion result.
     *
     * @param command isolated execution command
     * @return SQL execution result future
     */
    CompletionStage<SqlExecutionResult> executeIsolatedAsync(IsolatedExecuteCommand command);

    /**
     * Executes SQL synchronously in a clean isolated environment.
     *
     * @param command isolated execution command
     * @return SQL execution result
     */
    SqlExecutionResult executeIsolated(IsolatedExecuteCommand command);

    /**
     * Cancels a running SQL execution task.
     *
     * @param executionId execution task ID
     */
    void cancel(JudgeExecutionId executionId);

    /**
     * Drops a SQL execution environment.
     *
     * @param environmentId execution environment ID
     */
    void drop(JudgeEnvironmentId environmentId);
}
