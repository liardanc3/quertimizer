package com.quertimizer.auth.domain.model;

import lombok.Getter;

@Getter
public enum LoginFailReason {

    INVALID_EMAIL_OR_PASSWORD("이메일과 비밀번호를 확인해주세요."),
    BLOCKED_USER("차단된 계정입니다.");

    private final String message;

    LoginFailReason(String message) {
        this.message = message;
    }
}
