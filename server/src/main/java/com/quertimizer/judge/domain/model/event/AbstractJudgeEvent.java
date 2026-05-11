package com.quertimizer.judge.domain.model.event;

import com.quertimizer.judge.domain.entity.JudgeExecutionId;

import java.util.Objects;

public abstract class AbstractJudgeEvent implements JudgeEvent {

    private final JudgeExecutionId executionId;

    protected AbstractJudgeEvent(JudgeExecutionId executionId) {
        this.executionId = Objects.requireNonNull(executionId, "필수 값이 없습니다.");
    }

    @Override
    public JudgeExecutionId getExecutionId() {
        return executionId;
    }
}
