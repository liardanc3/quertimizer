package com.quertimizer.ui.domain.model;

public enum UiTextFailReason {

    DUPLICATED_UI_TEXT("이미 존재하는 UI 텍스트다."),
    UI_TEXT_NOT_FOUND("존재하지 않는 UI 텍스트다."),
    VALUE_REQUIRED("값이 필요하다."),
    DESCRIPTION_REQUIRED("설명이 필요하다."),
    KEY_REQUIRED("key가 필요하다."),
    LANGUAGE_REQUIRED("language가 필요하다.");

    private final String message;

    UiTextFailReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        // 메시지 조회
        return message;
    }

}
