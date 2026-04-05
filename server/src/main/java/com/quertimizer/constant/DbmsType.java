package com.quertimizer.constant;

public enum DbmsType {
    POSTGRESQL("postgresql"),
    ORACLE("oracle");

    private final String value;

    DbmsType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}
