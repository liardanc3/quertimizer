package com.quertimizer.admin.domain.model;

public enum AdminAnomalyFailReason {

    START_AFTER_END("시작 일시는 종료 일시보다 늦을 수 없습니다."),
    UNSUPPORTED_RANGE("지원하지 않는 조회 범위입니다."),
    CUSTOM_RANGE_REQUIRED("%s 일시를 입력해 주세요."),
    CUSTOM_RANGE_FORMAT_INVALID("%s 일시 형식이 올바르지 않습니다.");

    private final String message;

    AdminAnomalyFailReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String format(Object... args) {
        return message.formatted(args);
    }

}
