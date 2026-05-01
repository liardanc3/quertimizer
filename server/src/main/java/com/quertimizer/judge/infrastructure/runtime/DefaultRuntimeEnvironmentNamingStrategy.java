package com.quertimizer.judge.infrastructure.runtime;

import com.quertimizer.judge.domain.entity.ids.JudgeDatasetId;
import com.quertimizer.judge.domain.entity.ids.JudgeEnvironmentId;

import java.util.Locale;
import java.util.Objects;

public class DefaultRuntimeEnvironmentNamingStrategy implements RuntimeEnvironmentNamingStrategy {

    @Override
    public RuntimeEnvironmentName createName(JudgeEnvironmentId environmentId, JudgeDatasetId datasetId) {
        Objects.requireNonNull(environmentId, "environmentId must not be null");
        Objects.requireNonNull(datasetId, "datasetId must not be null");

        String normalizedName = ("judge_" + environmentId.getValue() + "_" + datasetId.getValue())
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9_]", "_");
        if (Character.isDigit(normalizedName.charAt(0))) {
            normalizedName = "e_" + normalizedName;
        }

        return new RuntimeEnvironmentName(normalizedName.length() > JudgeRuntimeConstants.MAX_ENVIRONMENT_NAME_LENGTH
                ? normalizedName.substring(0, JudgeRuntimeConstants.MAX_ENVIRONMENT_NAME_LENGTH)
                : normalizedName);
    }
}
