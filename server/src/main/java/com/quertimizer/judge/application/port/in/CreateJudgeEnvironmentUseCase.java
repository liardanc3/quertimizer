package com.quertimizer.judge.application.port.in;

import com.quertimizer.judge.application.input.CreateJudgeEnvironmentInput;
import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;

public interface CreateJudgeEnvironmentUseCase {

    JudgeEnvironmentId execute(CreateJudgeEnvironmentInput input);
}
