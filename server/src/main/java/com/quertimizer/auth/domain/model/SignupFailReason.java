package com.quertimizer.auth.domain.model;

public enum SignupFailReason {

    DUPLICATED_HANDLE("이미 사용중인 Handle입니다."),
    DUPLICATED_EMAIL("이미 사용중인 이메일입니다.");

    private final String message;

    SignupFailReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

}
