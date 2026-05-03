package com.quertimizer.judge.adapter.out.execution;

import com.quertimizer.judge.application.port.out.JudgeTemplateStorePort;
import com.quertimizer.judge.domain.entity.DatasetTemplateDefinition;
import com.quertimizer.judge.domain.entity.JudgeDatasetId;

import java.util.Optional;

public class NoOpJudgeTemplateStore implements JudgeTemplateStorePort {

    @Override
    public void saveDatasetTemplate(DatasetTemplateDefinition templateDefinition) {
    }

    @Override
    public Optional<DatasetTemplateDefinition> findDatasetTemplate(JudgeDatasetId datasetId) {
        return Optional.empty();
    }

    @Override
    public void deleteDatasetTemplate(JudgeDatasetId datasetId) {
    }
}
