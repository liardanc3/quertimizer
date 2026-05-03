package com.quertimizer.judge.adapter.out.execution;

import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.DatasetTemplateDefinition;

import java.util.Optional;

public interface DatasetTemplateProvisioner {

    Optional<DatasetTemplateDefinition> prepare(DatasetDefinition dataset);

    void drop(DatasetTemplateDefinition templateDefinition);
}
