package com.quertimizer.judge.application.input;

import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.model.EnvironmentPolicy;
import com.quertimizer.judge.domain.model.QueuePriority;
import com.quertimizer.judge.domain.model.QueueStatusListener;
import lombok.Data;

@Data
public class CreateEnvironmentInput {

    private final JudgeDatasetId datasetId;
    private final EnvironmentPolicy policy;
    private final QueuePriority queuePriority;
    private final QueueStatusListener queueStatusListener;

    public CreateEnvironmentInput(JudgeDatasetId datasetId, EnvironmentPolicy policy) {
        this(datasetId, policy, QueuePriority.NORMAL, QueueStatusListener.noop());
    }

    public CreateEnvironmentInput(JudgeDatasetId datasetId, EnvironmentPolicy policy,
                                  QueuePriority queuePriority,
                                  QueueStatusListener queueStatusListener) {
        this.datasetId = datasetId;
        this.policy = policy;
        this.queuePriority = queuePriority;
        this.queueStatusListener = queueStatusListener;
    }
}
