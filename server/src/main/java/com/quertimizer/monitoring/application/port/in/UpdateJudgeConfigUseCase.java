package com.quertimizer.monitoring.application.port.in;

import com.quertimizer.monitoring.application.input.JudgeConfigUpdateInput;
import com.quertimizer.monitoring.application.output.JudgeConfigOutput;

public interface UpdateJudgeConfigUseCase {

    JudgeConfigOutput execute(JudgeConfigUpdateInput input);
}
