package com.quertimizer.judge.application.port.in;

import com.quertimizer.judge.application.input.CreateDataset;
import com.quertimizer.judge.domain.entity.JudgeDatasetId;

public interface CreateJudgeDatasetUseCase {

    JudgeDatasetId execute(CreateDataset input);
}
