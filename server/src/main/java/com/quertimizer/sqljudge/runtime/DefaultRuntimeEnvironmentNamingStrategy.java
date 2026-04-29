package com.quertimizer.sqljudge.runtime;

import com.quertimizer.sqljudge.id.JudgeDatasetId;
import com.quertimizer.sqljudge.id.JudgeEnvironmentId;

import java.util.Locale;
import java.util.Objects;

/**
 * Creates deterministic internal runtime environment names from sql-judge IDs.
 */
public class DefaultRuntimeEnvironmentNamingStrategy implements RuntimeEnvironmentNamingStrategy {

    private static final int MAX_NAME_LENGTH = 63;

    /**
     * Creates an internal runtime environment name.
     *
     * @param environmentId execution environment ID
     * @param datasetId registered dataset ID
     * @return internal runtime environment name
     */
    @Override
    public RuntimeEnvironmentName createName(JudgeEnvironmentId environmentId, JudgeDatasetId datasetId) {
        Objects.requireNonNull(environmentId, "environmentId must not be null");
        Objects.requireNonNull(datasetId, "datasetId must not be null");

        String normalizedName = ("sqljudge_" + environmentId.getValue() + "_" + datasetId.getValue())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]", "_");
        if (Character.isDigit(normalizedName.charAt(0))) {
            normalizedName = "e_" + normalizedName;
        }

        return new RuntimeEnvironmentName(normalizedName.length() > MAX_NAME_LENGTH
                ? normalizedName.substring(0, MAX_NAME_LENGTH)
                : normalizedName);
    }
}
