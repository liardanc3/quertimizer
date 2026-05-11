package com.quertimizer.user.domain.model;

public enum UserProfileFailReason {

    USER_NOT_FOUND("존재하지 않는 사용자입니다.");

    private final String message;

    UserProfileFailReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        // 실패 메시지 반환
        return message;
    }
}
