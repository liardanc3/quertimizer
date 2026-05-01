package com.quertimizer.judge.infrastructure.runtime;

import com.quertimizer.judge.application.port.JudgeTemplateStore;
import com.quertimizer.judge.domain.entity.DatasetTemplateDefinition;
import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;

import java.util.Optional;

public class NoOpJudgeTemplateStore implements JudgeTemplateStore {

    @Override
    public void saveDatasetTemplate(DatasetTemplateDefinition templateDefinition) {
    }

    @Override
    public Optional<DatasetTemplateDefinition> findDatasetTemplate(JudgeDatasetId datasetId) {
        return Optional.empty();
    }
}
