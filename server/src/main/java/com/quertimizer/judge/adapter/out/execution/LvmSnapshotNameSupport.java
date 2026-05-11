package com.quertimizer.judge.adapter.out.execution;

import com.quertimizer.judge.domain.model.DbmsType;

public final class LvmSnapshotNameSupport {

    private LvmSnapshotNameSupport() {
    }

    public static String scriptDbmsName(DbmsType dbmsType) {
        return switch (dbmsType) {
            case POSTGRESQL -> "postgresql";
            case MYSQL -> "mysql";
        };
    }

    public static String scriptName(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("스크립트 이름 값이 비어 있습니다.");
        }

        StringBuilder normalizedName = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            normalizedName.append(isScriptNameCharacter(character) ? character : '_');
        }

        return normalizedName.toString();
    }

    public static RuntimeEnvironmentName datasetEnvironmentName(String datasetId) {
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
