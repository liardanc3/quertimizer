package com.quertimizer.judge.application.input;

import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;
import com.quertimizer.judge.domain.entity.JudgeExecutionId;
import com.quertimizer.judge.domain.model.ExecutionOptions;

import java.util.Objects;

public class AnalyzeJudgeEnvironmentInput {

    private final JudgeExecutionId executionId;
    private final JudgeEnvironmentId environmentId;
    private final ExecutionOptions options;

    public AnalyzeJudgeEnvironmentInput(JudgeExecutionId executionId, JudgeEnvironmentId environmentId,
                                     ExecutionOptions options) {
        this.executionId = Objects.requireNonNull(executionId, "필수 값이 없습니다.");
        this.environmentId = Objects.requireNonNull(environmentId, "필수 값이 없습니다.");
        this.options = Objects.requireNonNull(options, "필수 값이 없습니다.");
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
