package com.quertimizer.alarm.domain.model;

public enum AlarmTemplateFailReason {

    ALARM_TEMPLATE_NOT_FOUND("존재하지 않는 알람 템플릿이다."),
    SENTENCE_REQUIRED("표현식이 필요하다."),
    DESCRIPTION_REQUIRED("설명이 필요하다.");

    private final String message;

    AlarmTemplateFailReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        // 메시지 조회
        return message;
    }

}
