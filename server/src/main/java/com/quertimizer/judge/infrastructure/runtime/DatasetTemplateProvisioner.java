package com.quertimizer.judge.infrastructure.runtime;

import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.DatasetTemplateDefinition;

import java.util.Optional;

public interface DatasetTemplateProvisioner {

    Optional<DatasetTemplateDefinition> prepare(DatasetDefinition dataset);
}
