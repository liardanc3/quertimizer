package com.quertimizer.sqljudge.runtime;

import com.quertimizer.sqljudge.id.JudgeDatasetId;
import com.quertimizer.sqljudge.id.JudgeEnvironmentId;

/**
 * Creates internal runtime environment names.
 */
public interface RuntimeEnvironmentNamingStrategy {

    /**
     * Creates an internal runtime environment name.
     *
     * @param environmentId execution environment ID
     * @param datasetId registered dataset ID
     * @return internal runtime environment name
     */
    RuntimeEnvironmentName createName(JudgeEnvironmentId environmentId, JudgeDatasetId datasetId);
}
