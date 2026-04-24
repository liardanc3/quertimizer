package com.quertimizer.auth.domain.model;

import lombok.Getter;

@Getter
public enum LoginFailReason {

    INVALID_EMAIL_OR_PASSWORD("이메일 또는 비밀번호가 올바르지 않습니다."),
    BLOCKED_USER("차단된 계정입니다.");

    private final String message;

    LoginFailReason(String message) {
        this.message = message;
    }
}
