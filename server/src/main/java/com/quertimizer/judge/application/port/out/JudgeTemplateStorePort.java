package com.quertimizer.judge.application.port.out;

import com.quertimizer.judge.domain.entity.DatasetTemplateDefinition;
import com.quertimizer.judge.domain.entity.JudgeDatasetId;

import java.util.Optional;

public interface JudgeTemplateStorePort {

    void saveDatasetTemplate(DatasetTemplateDefinition templateDefinition);

    Optional<DatasetTemplateDefinition> findDatasetTemplate(JudgeDatasetId datasetId);
}
