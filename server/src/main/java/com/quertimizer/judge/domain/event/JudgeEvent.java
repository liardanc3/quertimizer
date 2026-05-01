package com.quertimizer.judge.domain.event;

import com.quertimizer.judge.domain.entity.ids.JudgeExecutionId;

public interface JudgeEvent {

    JudgeExecutionId getExecutionId();
}
