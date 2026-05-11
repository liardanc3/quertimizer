package com.quertimizer.judge.adapter.out.execution;

import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.DatasetTemplateDefinition;
import com.quertimizer.judge.domain.model.JudgeQueuePriority;

import java.util.Optional;

public interface DatasetTemplateProvisioner {

    Optional<DatasetTemplateDefinition> prepare(DatasetDefinition dataset);

    default Optional<DatasetTemplateDefinition> prepare(DatasetDefinition dataset, JudgeQueuePriority queuePriority) {
        return prepare(dataset);
    }

    void drop(DatasetTemplateDefinition templateDefinition);
}
