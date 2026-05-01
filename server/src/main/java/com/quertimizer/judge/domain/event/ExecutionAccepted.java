package com.quertimizer.judge.domain.event;

import com.quertimizer.judge.domain.entity.ids.JudgeExecutionId;

public class ExecutionAccepted extends AbstractJudgeEvent {

    public ExecutionAccepted(JudgeExecutionId executionId) {
        super(executionId);
    }
}
