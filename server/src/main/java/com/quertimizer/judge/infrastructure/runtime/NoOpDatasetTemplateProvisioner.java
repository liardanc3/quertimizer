package com.quertimizer.judge.infrastructure.runtime;

import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.DatasetTemplateDefinition;

import java.util.Optional;

public class NoOpDatasetTemplateProvisioner implements DatasetTemplateProvisioner {

    @Override
    public Optional<DatasetTemplateDefinition> prepare(DatasetDefinition dataset) {
        return Optional.empty();
    }
}
