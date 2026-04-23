package com.quertimizer.ui.domain.model;

public enum UiTextKey {

    TITLE("TITLE"),
    NOTIFICATION("NOTIFICATION");

    private final String value;

    UiTextKey(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

}
