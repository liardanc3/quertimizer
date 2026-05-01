package com.quertimizer.judge.domain.event;

import com.quertimizer.judge.domain.entity.ids.JudgeExecutionId;

import java.util.Objects;

public abstract class AbstractJudgeEvent implements JudgeEvent {

    private final JudgeExecutionId executionId;

    protected AbstractJudgeEvent(JudgeExecutionId executionId) {
        this.executionId = Objects.requireNonNull(executionId, "executionId must not be null");
    }

    @Override
    public JudgeExecutionId getExecutionId() {
        return executionId;
    }
}
