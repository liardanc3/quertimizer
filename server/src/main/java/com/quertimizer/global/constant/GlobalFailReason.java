package com.quertimizer.global.constant;

import lombok.Getter;

@Getter
public enum GlobalFailReason {

    UNEXPECTED_ERROR("잠시 후 다시 시도해 주세요."),
    BAD_REQUEST("잘못된 요청입니다."),
    AUTHENTICATION_REQUIRED("로그인이 필요합니다. 다시 로그인해 주세요."),
    ACCESS_DENIED("접근 권한이 없습니다."),
    LOCK_ACQUIRE_FAILED("잠시 후 다시 시도해 주세요."),
    LOCK_KEY_RESOLVE_FAILED("Resolved lock key is null. expression=%s"),
    LOCK_ENTRY_NOT_FOUND("No lock entry found for key: %s");

    private final String message;

    GlobalFailReason(String message) {
        this.message = message;
    }

    public String format(Object... args) {
        // 메시지 포맷 적용
        return message.formatted(args);
    }

}
