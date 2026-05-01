package com.quertimizer.judge.domain.event;

import com.quertimizer.judge.domain.entity.ids.JudgeExecutionId;

public class ExecutionFailed extends AbstractJudgeEvent {

    private final String message;
    private final Throwable cause;

    public ExecutionFailed(JudgeExecutionId executionId, String message, Throwable cause) {
        super(executionId);
        this.message = message == null || message.isBlank() ? "SQL execution failed" : message;
        this.cause = cause;
    }

    public String getMessage() {
        return message;
    }

    public Throwable getCause() {
        return cause;
    }
}
