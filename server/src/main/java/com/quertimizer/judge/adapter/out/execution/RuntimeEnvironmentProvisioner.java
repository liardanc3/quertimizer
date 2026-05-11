package com.quertimizer.judge.adapter.out.execution;

import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;
import com.quertimizer.judge.domain.model.EnvironmentPolicy;
import com.quertimizer.judge.domain.model.JudgeQueuePriority;
import com.quertimizer.judge.domain.model.JudgeQueueStatusListener;

public interface RuntimeEnvironmentProvisioner {

    ProvisionedRuntimeEnvironment create(JudgeEnvironmentId environmentId, DatasetDefinition dataset, EnvironmentPolicy policy);

    default ProvisionedRuntimeEnvironment create(JudgeEnvironmentId environmentId, DatasetDefinition dataset,
                                                 EnvironmentPolicy policy, JudgeQueuePriority queuePriority,
                                                 JudgeQueueStatusListener queueStatusListener) {
        return create(environmentId, dataset, policy);
    }

    RuntimeEnvironmentConnection openConnection(ProvisionedRuntimeEnvironment environment, int timeoutSeconds);

    void drop(ProvisionedRuntimeEnvironment environment);
}
