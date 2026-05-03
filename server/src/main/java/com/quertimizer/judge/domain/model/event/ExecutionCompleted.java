package com.quertimizer.judge.domain.model.event;

import com.quertimizer.judge.domain.entity.JudgeExecutionId;
import com.quertimizer.judge.application.output.SqlExecutionResult;

import java.util.Objects;

public class ExecutionCompleted extends AbstractJudgeEvent {

    private final SqlExecutionResult result;

    public ExecutionCompleted(JudgeExecutionId executionId, SqlExecutionResult result) {
        super(executionId);
        this.result = Objects.requireNonNull(result, "필수 값이 없다.");
    }

    public SqlExecutionResult getResult() {
        return result;
    }
}
