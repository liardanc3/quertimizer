package com.quertimizer.judge.application.model;

import com.quertimizer.judge.domain.model.DbmsType;

import java.util.Locale;

public final class Names {

    private Names() {
    }

    public static String scriptDbmsName(DbmsType dbmsType) {
        return switch (dbmsType) {
            case POSTGRESQL -> "postgresql";
            case MYSQL -> "mysql";
        };
    }

    public static String scriptName(String value) {
        // 스크립트 이름 허용 문자 기준으로 정규화
        StringBuilder normalizedName = new StringBuilder(value.length());
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            normalizedName.append(isScriptNameCharacter(character) ? character : '_');
        }

        return normalizedName.toString();
    }

    public static String datasetEnvironmentName(String datasetId) {
        // 데이터셋 ID 기준 실행 환경 이름 구성
        String normalizedName = ("judge_dataset_" + scriptName(datasetId)).toLowerCase(Locale.ROOT).replace('-', '_');
        if (Character.isDigit(normalizedName.charAt(0))) {
            normalizedName = "d_" + normalizedName;
        }

        // DBMS별 identifier 길이 제한에 맞춰 반환
        return normalizedName.length() > Constants.MAX_ENVIRONMENT_NAME_LENGTH
                ? normalizedName.substring(0, Constants.MAX_ENVIRONMENT_NAME_LENGTH)
                : normalizedName;
    }

    private static boolean isScriptNameCharacter(char character) {
        return (character >= 'a' && character <= 'z')
                || (character >= 'A' && character <= 'Z')
                || (character >= '0' && character <= '9')
                || character == '_'
                || character == '-';
    }
}
