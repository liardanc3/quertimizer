package com.quertimizer.judge.application.port.in;

import com.quertimizer.judge.application.input.CreateJudgeSetupSqlInput;
import com.quertimizer.judge.domain.entity.JudgeSetupSqlId;

public interface CreateJudgeSetupSqlUseCase {

    JudgeSetupSqlId execute(CreateJudgeSetupSqlInput input);
}
