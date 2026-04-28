package com.quertimizer.global.constant;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

public enum DbmsType {
    POSTGRESQL("postgresql", "PostgreSQL", "P"),
    MYSQL("mysql", "MySQL", "M");

    private final String value;
    private final String label;
    private final String idPrefix;

    DbmsType(String value, String label, String idPrefix) {
        this.value = value;
        this.label = label;
        this.idPrefix = idPrefix;
    }

    public String getValue() {
        // 저장 값을 반환한다
        return value;
    }

    public String getLabel() {
        // 표시명 조회
        return label;
    }

    public String getIdPrefix() {
        // 문제 번호 Prefix 조회
        return idPrefix;
    }

    public static Optional<DbmsType> fromValue(String value) {
        // 값으로 DBMS 유형 조회
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }

        String normalizedValue = value.trim();
        return Arrays.stream(values())
                .filter(dbmsType -> dbmsType.value.equalsIgnoreCase(normalizedValue))
                .findFirst();
    }

    public static DbmsType fromValueOrDefault(String value, DbmsType defaultType) {
        // 값으로 DBMS 유형을 조회하고 없으면 기본값 사용
        return fromValue(value).orElse(defaultType);
    }

    public static Optional<DbmsType> fromPrefix(String prefix) {
        // Prefix로 DBMS 유형 조회
        if (prefix == null || prefix.isBlank()) {
            return Optional.empty();
        }

        String normalizedPrefix = prefix.trim().substring(0, 1);
        return Arrays.stream(values())
                .filter(dbmsType -> dbmsType.idPrefix.equalsIgnoreCase(normalizedPrefix))
                .findFirst();
    }

    public static Optional<DbmsType> fromScopedId(String scopedId) {
        // 스코프 ID로 DBMS 유형 조회
        if (scopedId == null || scopedId.isBlank()) {
            return Optional.empty();
        }

        return fromPrefix(scopedId.trim().substring(0, 1));
    }

    public static String supportedIdPrefixPattern() {
        // 지원 Prefix 정규식 조각 생성
        return Arrays.stream(values())
                .map(DbmsType::getIdPrefix)
                .collect(Collectors.joining());
    }

    public static boolean isScopedProblemSetId(String problemSetId) {
        // 스코프 문제 테이블셋 번호 여부 확인
        if (problemSetId == null || problemSetId.isBlank()) {
            return false;
        }

        String normalizedProblemSetId = problemSetId.trim().toUpperCase();
        return fromScopedId(normalizedProblemSetId).isPresent()
                && normalizedProblemSetId.substring(1).matches("\\d{5}");
    }

    public static boolean isScopedProblemId(String problemId) {
        // 스코프 문제 번호 여부 확인
        if (problemId == null || problemId.isBlank()) {
            return false;
        }

        String[] tokens = problemId.trim().toUpperCase().split("-");
        return tokens.length == 2 && isScopedProblemSetId(tokens[0]) && tokens[1].matches("\\d{5}");
    }

    public static String extractBaseProblemSetId(String scopedValue) {
        // 스코프 값에서 기준 문제 테이블셋 번호 추출
        if (scopedValue == null || scopedValue.isBlank()) {
            return "";
        }

        String[] tokens = scopedValue.trim().toUpperCase().split("-");
        String scopedProblemSetId = tokens.length > 0 ? tokens[0] : scopedValue.trim().toUpperCase();
        return isScopedProblemSetId(scopedProblemSetId) ? scopedProblemSetId.substring(1) : scopedProblemSetId;
    }
}
