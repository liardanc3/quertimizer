package com.quertimizer.alarm.domain.model;

public enum AlarmTemplateFailReason {

    ALARM_TEMPLATE_NOT_FOUND("존재하지 않는 알람 템플릿입니다."),
    SENTENCE_REQUIRED("표현식이 필요합니다."),
    DESCRIPTION_REQUIRED("설명이 필요합니다.");

    private final String message;

    AlarmTemplateFailReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
