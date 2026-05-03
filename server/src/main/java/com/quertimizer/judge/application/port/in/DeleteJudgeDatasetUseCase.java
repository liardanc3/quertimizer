package com.quertimizer.judge.application.port.in;

import com.quertimizer.judge.domain.entity.JudgeDatasetId;

public interface DeleteJudgeDatasetUseCase {

    void execute(JudgeDatasetId datasetId);
}
