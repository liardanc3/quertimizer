package com.quertimizer.ui.domain.model;

public enum UiTextLanguage {

    DEFAULT("default");

    private final String value;

    UiTextLanguage(String value) {
        this.value = value;
    }

    public String getValue() {
        // 저장 값을 반환한다
        return value;
    }

}
