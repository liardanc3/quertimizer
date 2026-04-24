package com.quertimizer.ui.domain.model;

public enum UiTextLanguage {

    DEFAULT("default");

    private final String value;

    UiTextLanguage(String value) {
        this.value = value;
    }

    public String getValue() {
        // 값 조회
        return value;
    }

}
