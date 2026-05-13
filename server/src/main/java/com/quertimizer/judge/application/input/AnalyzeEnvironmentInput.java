package com.quertimizer.judge.application.input;

import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;
import com.quertimizer.judge.domain.entity.JudgeExecutionId;
import com.quertimizer.judge.domain.model.ExecutionOptions;
import lombok.Data;

@Data
public class AnalyzeEnvironmentInput {

    private final JudgeExecutionId executionId;
    private final JudgeEnvironmentId environmentId;
    private final ExecutionOptions options;

    public AnalyzeEnvironmentInput(JudgeExecutionId executionId, JudgeEnvironmentId environmentId,
                                        ExecutionOptions options) {
        this.executionId = executionId;
        this.environmentId = environmentId;
        this.options = options;
    }
}
