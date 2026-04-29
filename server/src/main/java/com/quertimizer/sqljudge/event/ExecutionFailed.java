package com.quertimizer.sqljudge.event;

import com.quertimizer.sqljudge.id.JudgeExecutionId;

/**
 * Indicates that a SQL execution has failed.
 */
public class ExecutionFailed extends AbstractSqlJudgeEvent {

    private final String message;
    private final Throwable cause;

    /**
     * Creates a SQL execution failed event.
     *
     * @param executionId execution task ID
     * @param message failure message
     * @param cause failure cause
     */
    public ExecutionFailed(JudgeExecutionId executionId, String message, Throwable cause) {
        super(executionId);
        this.message = message == null || message.isBlank() ? "SQL execution failed" : message;
        this.cause = cause;
    }

    /**
     * Returns the failure message.
     *
     * @return failure message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the failure cause.
     *
     * @return failure cause
     */
    public Throwable getCause() {
        return cause;
    }
}
