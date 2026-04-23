package com.quertimizer.global.constant;

public enum GlobalFailReason {

    UNEXPECTED_ERROR("잠시 후 다시 시도해 주세요."),
    BAD_REQUEST("잘못된 요청입니다."),
    LOCK_ACQUIRE_FAILED("잠시 후 다시 시도해 주세요."),
    LOCK_KEY_RESOLVE_FAILED("Resolved lock key is null. expression=%s"),
    LOCK_ENTRY_NOT_FOUND("No lock entry found for key: %s");

    private final String message;

    GlobalFailReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String format(Object... args) {
        return message.formatted(args);
    }

}
