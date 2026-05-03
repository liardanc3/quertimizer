package com.quertimizer.judge.domain.model.event;

import com.quertimizer.judge.domain.entity.JudgeExecutionId;

public class ExecutionAccepted extends AbstractJudgeEvent {

    public ExecutionAccepted(JudgeExecutionId executionId) {
        super(executionId);
    }
}
