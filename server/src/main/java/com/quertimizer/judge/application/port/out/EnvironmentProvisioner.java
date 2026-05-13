package com.quertimizer.judge.application.port.out;

import com.quertimizer.judge.application.model.ProvisionedEnvironment;
import com.quertimizer.judge.application.model.EnvironmentConnection;
import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;
import com.quertimizer.judge.domain.model.EnvironmentPolicy;
import com.quertimizer.judge.domain.model.QueuePriority;
import com.quertimizer.judge.domain.model.QueueStatusListener;

public interface EnvironmentProvisioner {

    ProvisionedEnvironment create(JudgeEnvironmentId environmentId, DatasetDefinition dataset, EnvironmentPolicy policy);

    default ProvisionedEnvironment create(JudgeEnvironmentId environmentId, DatasetDefinition dataset,
                                                 EnvironmentPolicy policy, QueuePriority queuePriority,
                                                 QueueStatusListener queueStatusListener) {
        return create(environmentId, dataset, policy);
    }

    EnvironmentConnection openConnection(ProvisionedEnvironment environment, int timeoutSeconds);

    void drop(ProvisionedEnvironment environment);
}
