package com.quertimizer.global.constant;

public enum DbmsType {
    POSTGRESQL("postgresql"),
    ORACLE("oracle");

    private final String value;

    DbmsType(String value) {
        this.value = value;
    }

    public String getValue() {
        // 값 조회
        return value;
    }
}
