package com.quertimizer.judge.infrastructure.runtime;

import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.ids.JudgeEnvironmentId;

public interface RuntimeEnvironmentNamingStrategy {

    RuntimeEnvironmentName createName(JudgeEnvironmentId environmentId, JudgeDatasetId datasetId);
}
