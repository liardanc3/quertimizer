package com.quertimizer.judge.infrastructure.runtime;

import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.ids.JudgeEnvironmentId;
import com.quertimizer.judge.domain.model.EnvironmentPolicy;

public interface RuntimeEnvironmentProvisioner {

    ProvisionedRuntimeEnvironment create(JudgeEnvironmentId environmentId, DatasetDefinition dataset, EnvironmentPolicy policy);

    RuntimeEnvironmentConnection openConnection(ProvisionedRuntimeEnvironment environment, int timeoutSeconds);

    void drop(ProvisionedRuntimeEnvironment environment);
}
