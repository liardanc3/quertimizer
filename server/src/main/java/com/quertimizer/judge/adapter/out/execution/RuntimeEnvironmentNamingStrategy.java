package com.quertimizer.judge.adapter.out.execution;

import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.JudgeEnvironmentId;

public interface RuntimeEnvironmentNamingStrategy {

    RuntimeEnvironmentName createName(JudgeEnvironmentId environmentId, JudgeDatasetId datasetId);
}
