package com.quertimizer.judge.application.service;

import com.quertimizer.judge.application.port.in.HasJudgeDatasetUseCase;
import com.quertimizer.judge.application.port.out.JudgeDefinitionStorePort;
import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class HasJudgeDataset implements HasJudgeDatasetUseCase {

    private final JudgeDefinitionStorePort judgeDefinitionStore;

    /**
     * judge 데이터셋 존재 여부를 확인한다.
     *
     * @param datasetId 확인할 데이터셋 ID
     * @return 데이터셋 존재 여부
     */
    @Override
    public boolean execute(String datasetId) {
        return judgeDefinitionStore.findDataset(new JudgeDatasetId(datasetId)).isPresent();
    }
}
