package com.quertimizer.judge.application.service;

import com.quertimizer.judge.application.port.in.DeleteJudgeDatasetUseCase;
import com.quertimizer.judge.application.port.out.JudgeRuntimePort;
import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeleteJudgeDataset implements DeleteJudgeDatasetUseCase {

    private final JudgeRuntimePort judgeRuntime;

    /**
     * judge 데이터셋을 제거한다.
     *
     * @param datasetId 제거할 데이터셋 ID
     */
    @Override
    public void execute(JudgeDatasetId datasetId) {
        judgeRuntime.deleteDataset(datasetId);
    }
}
