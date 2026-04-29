package com.quertimizer.sqljudge.definition;

import com.quertimizer.sqljudge.id.JudgeDatasetId;
import com.quertimizer.sqljudge.id.JudgeReferenceId;
import com.quertimizer.sqljudge.id.JudgeSetupSqlId;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stores sql-judge definitions in memory for the first extraction phase.
 */
public class InMemorySqlJudgeDefinitionStore implements SqlJudgeDefinitionStore {

    private final ConcurrentHashMap<JudgeDatasetId, DatasetDefinition> datasets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<JudgeSetupSqlId, SetupSqlDefinition> setupSqls = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<JudgeReferenceId, ReferenceDefinition> references = new ConcurrentHashMap<>();

    /**
     * Saves a dataset definition.
     *
     * @param datasetDefinition dataset definition
     */
    @Override
    public void saveDataset(DatasetDefinition datasetDefinition) {
        Objects.requireNonNull(datasetDefinition, "datasetDefinition must not be null");

        datasets.put(datasetDefinition.getDatasetId(), datasetDefinition);
    }

    /**
     * Finds a dataset definition by ID.
     *
     * @param datasetId dataset ID
     * @return dataset definition when it exists
     */
    @Override
    public Optional<DatasetDefinition> findDataset(JudgeDatasetId datasetId) {
        Objects.requireNonNull(datasetId, "datasetId must not be null");

        return Optional.ofNullable(datasets.get(datasetId));
    }

    /**
     * Saves a setup SQL definition.
     *
     * @param setupSqlDefinition setup SQL definition
     */
    @Override
    public void saveSetupSql(SetupSqlDefinition setupSqlDefinition) {
        Objects.requireNonNull(setupSqlDefinition, "setupSqlDefinition must not be null");

        setupSqls.put(setupSqlDefinition.getSetupSqlId(), setupSqlDefinition);
    }

    /**
     * Finds a setup SQL definition by ID.
     *
     * @param setupSqlId setup SQL bundle ID
     * @return setup SQL definition when it exists
     */
    @Override
    public Optional<SetupSqlDefinition> findSetupSql(JudgeSetupSqlId setupSqlId) {
        Objects.requireNonNull(setupSqlId, "setupSqlId must not be null");

        return Optional.ofNullable(setupSqls.get(setupSqlId));
    }

    /**
     * Saves a reference SQL definition.
     *
     * @param referenceDefinition reference SQL definition
     */
    @Override
    public void saveReference(ReferenceDefinition referenceDefinition) {
        Objects.requireNonNull(referenceDefinition, "referenceDefinition must not be null");

        references.put(referenceDefinition.getReferenceId(), referenceDefinition);
    }

    /**
     * Finds a reference SQL definition by ID.
     *
     * @param referenceId reference SQL ID
     * @return reference SQL definition when it exists
     */
    @Override
    public Optional<ReferenceDefinition> findReference(JudgeReferenceId referenceId) {
        Objects.requireNonNull(referenceId, "referenceId must not be null");

        return Optional.ofNullable(references.get(referenceId));
    }
}
