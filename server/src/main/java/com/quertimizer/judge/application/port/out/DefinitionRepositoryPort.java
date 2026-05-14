package com.quertimizer.judge.application.port.out;

import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.SetupSqlDefinition;
import com.quertimizer.judge.domain.entity.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.JudgeSetupSqlId;

import java.util.Optional;

public interface DefinitionRepositoryPort {

    DatasetDefinition saveDataset(DatasetDefinition datasetDefinition, boolean storeSqlDefinition);

    Optional<DatasetDefinition> findDataset(JudgeDatasetId datasetId);

    void deleteDataset(JudgeDatasetId datasetId);

    void saveSetupSql(SetupSqlDefinition setupSqlDefinition);

    Optional<SetupSqlDefinition> findSetupSql(JudgeSetupSqlId setupSqlId);
}
