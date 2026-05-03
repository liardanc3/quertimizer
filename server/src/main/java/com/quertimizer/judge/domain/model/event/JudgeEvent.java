package com.quertimizer.judge.domain.model.event;

import com.quertimizer.judge.domain.entity.JudgeExecutionId;

public interface JudgeEvent {

    JudgeExecutionId getExecutionId();
}
