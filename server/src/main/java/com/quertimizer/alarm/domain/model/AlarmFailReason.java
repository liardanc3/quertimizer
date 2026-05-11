package com.quertimizer.alarm.domain.model;

public enum AlarmFailReason {

    RECIPIENT_REQUIRED("수신자가 필요합니다."),
    MESSAGE_REQUIRED("알람 내용이 필요합니다."),
    HANDLE_NOT_FOUND("존재하지 않는 Handle이 포함되어 있습니다.");

    private final String message;

    AlarmFailReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
