package com.quertimizer.sqljudge.command;

import com.quertimizer.sqljudge.id.JudgeEnvironmentId;
import com.quertimizer.sqljudge.id.JudgeExecutionId;
import com.quertimizer.sqljudge.policy.ExecutionOptions;

import java.util.Objects;

/**
 * Carries SQL execution input for an existing environment.
 */
public class ExecuteSqlCommand {

    private final JudgeExecutionId executionId;
    private final JudgeEnvironmentId environmentId;
    private final String sql;
    private final ExecutionOptions options;

    /**
     * Creates a SQL execution command.
     *
     * @param executionId execution task ID
     * @param environmentId execution environment ID
     * @param sql SQL statement
     * @param options SQL execution options
     */
    public ExecuteSqlCommand(JudgeExecutionId executionId,
                             JudgeEnvironmentId environmentId,
                             String sql,
                             ExecutionOptions options) {
        this.executionId = Objects.requireNonNull(executionId, "executionId must not be null");
        this.environmentId = Objects.requireNonNull(environmentId, "environmentId must not be null");
        this.sql = requireSql(sql);
        this.options = Objects.requireNonNull(options, "options must not be null");
    }

    /**
     * Returns the execution task ID.
     *
     * @return execution task ID
     */
    public JudgeExecutionId getExecutionId() {
        return executionId;
    }

    /**
     * Returns the execution environment ID.
     *
     * @return execution environment ID
     */
    public JudgeEnvironmentId getEnvironmentId() {
        return environmentId;
    }

    /**
     * Returns the SQL statement.
     *
     * @return SQL statement
     */
    public String getSql() {
        return sql;
    }

    /**
     * Returns the SQL execution options.
     *
     * @return SQL execution options
     */
    public ExecutionOptions getOptions() {
        return options;
    }

    private String requireSql(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("sql must not be blank");
        }

        return sql;
    }
}
