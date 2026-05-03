package com.quertimizer.judge.application.port.in;

import com.quertimizer.judge.domain.entity.JudgeExecutionId;

public interface CancelJudgeExecutionUseCase {

    void execute(JudgeExecutionId executionId);
}
