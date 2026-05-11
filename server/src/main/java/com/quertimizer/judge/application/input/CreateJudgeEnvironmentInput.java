package com.quertimizer.judge.application.input;

import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.model.EnvironmentPolicy;
import com.quertimizer.judge.domain.model.JudgeQueuePriority;
import com.quertimizer.judge.domain.model.JudgeQueueStatusListener;

import java.util.Objects;

public class CreateJudgeEnvironmentInput {

    private final JudgeDatasetId datasetId;
    private final EnvironmentPolicy policy;
    private final JudgeQueuePriority queuePriority;
    private final JudgeQueueStatusListener queueStatusListener;

    public CreateJudgeEnvironmentInput(JudgeDatasetId datasetId, EnvironmentPolicy policy) {
        this(datasetId, policy, JudgeQueuePriority.NORMAL, JudgeQueueStatusListener.noop());
    }

    public CreateJudgeEnvironmentInput(JudgeDatasetId datasetId, EnvironmentPolicy policy,
                                       JudgeQueuePriority queuePriority,
                                       JudgeQueueStatusListener queueStatusListener) {
        this.datasetId = Objects.requireNonNull(datasetId, "필수 값이 없습니다.");
        this.policy = Objects.requireNonNull(policy, "필수 값이 없습니다.");
        this.queuePriority = Objects.requireNonNull(queuePriority, "필수 값이 없습니다.");
        this.queueStatusListener = Objects.requireNonNull(queueStatusListener, "필수 값이 없습니다.");
    }

    public JudgeDatasetId getDatasetId() {
        return datasetId;
    }

    public EnvironmentPolicy getPolicy() {
        return policy;
    }

    public JudgeQueuePriority getQueuePriority() {
        return queuePriority;
    }

    public JudgeQueueStatusListener getQueueStatusListener() {
        return queueStatusListener;
    }
}
