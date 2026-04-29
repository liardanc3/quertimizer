package com.quertimizer.sqljudge.definition;

import com.quertimizer.sqljudge.id.JudgeDatasetId;
import com.quertimizer.sqljudge.id.JudgeReferenceId;
import com.quertimizer.sqljudge.id.JudgeSetupSqlId;

import java.util.Optional;

/**
 * Stores registered dataset and setup SQL definitions for sql-judge executions.
 */
public interface SqlJudgeDefinitionStore {

    /**
     * Saves a dataset definition.
     *
     * @param datasetDefinition dataset definition
     */
    void saveDataset(DatasetDefinition datasetDefinition);

    /**
     * Finds a dataset definition by ID.
     *
     * @param datasetId dataset ID
     * @return dataset definition when it exists
     */
    Optional<DatasetDefinition> findDataset(JudgeDatasetId datasetId);

    /**
     * Saves a setup SQL definition.
     *
     * @param setupSqlDefinition setup SQL definition
     */
    void saveSetupSql(SetupSqlDefinition setupSqlDefinition);

    /**
     * Finds a setup SQL definition by ID.
     *
     * @param setupSqlId setup SQL bundle ID
     * @return setup SQL definition when it exists
     */
    Optional<SetupSqlDefinition> findSetupSql(JudgeSetupSqlId setupSqlId);

    /**
     * Saves a reference SQL definition.
     *
     * @param referenceDefinition reference SQL definition
     */
    void saveReference(ReferenceDefinition referenceDefinition);

    /**
     * Finds a reference SQL definition by ID.
     *
     * @param referenceId reference SQL ID
     * @return reference SQL definition when it exists
     */
    Optional<ReferenceDefinition> findReference(JudgeReferenceId referenceId);
}
