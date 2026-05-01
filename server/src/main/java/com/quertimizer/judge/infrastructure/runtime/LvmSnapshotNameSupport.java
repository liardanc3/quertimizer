package com.quertimizer.judge.infrastructure.runtime;

import com.quertimizer.judge.domain.model.DbmsType;

final class LvmSnapshotNameSupport {

    private LvmSnapshotNameSupport() {
    }

    static String scriptDbmsName(DbmsType dbmsType) {
        return switch (dbmsType) {
            case POSTGRESQL -> "postgresql";
            case MYSQL -> "mysql";
        };
    }

    static String scriptName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("script name value must not be blank");
        }

        StringBuilder normalizedName = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            normalizedName.append(isScriptNameCharacter(character) ? character : '_');
        }

        return normalizedName.toString();
    }

    static RuntimeEnvironmentName datasetEnvironmentName(String datasetId) {
        String normalizedName = ("judge_dataset_" + scriptName(datasetId))
                .toLowerCase(java.util.Locale.ROOT)
                .replace('-', '_');
        if (Character.isDigit(normalizedName.charAt(0))) {
            normalizedName = "d_" + normalizedName;
        }

        return new RuntimeEnvironmentName(normalizedName.length() > JudgeRuntimeConstants.MAX_ENVIRONMENT_NAME_LENGTH
                ? normalizedName.substring(0, JudgeRuntimeConstants.MAX_ENVIRONMENT_NAME_LENGTH)
                : normalizedName);
    }

    private static boolean isScriptNameCharacter(char character) {
        return (character >= 'a' && character <= 'z')
                || (character >= 'A' && character <= 'Z')
                || (character >= '0' && character <= '9')
                || character == '_'
                || character == '-';
    }
}
