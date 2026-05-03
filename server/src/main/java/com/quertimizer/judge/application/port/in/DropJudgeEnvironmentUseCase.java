package com.quertimizer.judge.application.port.in;

import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;

public interface DropJudgeEnvironmentUseCase {

    void execute(JudgeEnvironmentId environmentId);
}
