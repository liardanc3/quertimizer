package com.quertimizer.judge.application.input;

import com.quertimizer.judge.domain.entity.ids.JudgeEnvironmentId;
import com.quertimizer.judge.domain.entity.ids.JudgeExecutionId;
import com.quertimizer.judge.domain.model.ExecutionOptions;

import java.util.Objects;

public class AnalyzeJudgeEnvironmentInput {

    private final JudgeExecutionId executionId;
    private final JudgeEnvironmentId environmentId;
    private final ExecutionOptions options;

    public AnalyzeJudgeEnvironmentInput(JudgeExecutionId executionId, JudgeEnvironmentId environmentId,
                                     ExecutionOptions options) {
        this.executionId = Objects.requireNonNull(executionId, "executionId must not be null");
        this.environmentId = Objects.requireNonNull(environmentId, "environmentId must not be null");
        this.options = Objects.requireNonNull(options, "options must not be null");
    }

    public JudgeExecutionId getExecutionId() {
        return executionId;
    }

    public JudgeEnvironmentId getEnvironmentId() {
        return environmentId;
    }

    public ExecutionOptions getOptions() {
        return options;
    }
}
