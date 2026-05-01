package com.quertimizer.judge.domain.event;

import com.quertimizer.judge.domain.entity.ids.JudgeExecutionId;
import com.quertimizer.judge.application.output.SqlExecutionResult;

import java.util.Objects;

public class ExecutionCompleted extends AbstractJudgeEvent {

    private final SqlExecutionResult result;

    public ExecutionCompleted(JudgeExecutionId executionId, SqlExecutionResult result) {
        super(executionId);
        this.result = Objects.requireNonNull(result, "result must not be null");
    }

    public SqlExecutionResult getResult() {
        return result;
    }
}
