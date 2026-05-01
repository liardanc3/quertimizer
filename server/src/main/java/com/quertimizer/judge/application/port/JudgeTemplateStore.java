package com.quertimizer.judge.application.port;

import com.quertimizer.judge.domain.entity.DatasetTemplateDefinition;
import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;

import java.util.Optional;

public interface JudgeTemplateStore {

    void saveDatasetTemplate(DatasetTemplateDefinition templateDefinition);

    Optional<DatasetTemplateDefinition> findDatasetTemplate(JudgeDatasetId datasetId);
}
