package com.quertimizer.judge.application.port;

import com.quertimizer.judge.domain.entity.DatasetDefinition;
import com.quertimizer.judge.domain.entity.ReferenceDefinition;
import com.quertimizer.judge.domain.entity.SetupSqlDefinition;
import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.ids.JudgeReferenceId;
import com.quertimizer.judge.domain.entity.ids.JudgeSetupSqlId;

import java.util.Optional;

public interface JudgeDefinitionStore {

    void saveDataset(DatasetDefinition datasetDefinition);

    Optional<DatasetDefinition> findDataset(JudgeDatasetId datasetId);

    void saveSetupSql(SetupSqlDefinition setupSqlDefinition);

    Optional<SetupSqlDefinition> findSetupSql(JudgeSetupSqlId setupSqlId);

    void saveReference(ReferenceDefinition referenceDefinition);

    Optional<ReferenceDefinition> findReference(JudgeReferenceId referenceId);
}
