package com.quertimizer.auth.domain.model;

import lombok.Getter;

@Getter
public enum SignupFailReason {

    DUPLICATED_HANDLE("이미 사용중인 Handle 입니다."),
    DUPLICATED_EMAIL("이미 사용중인 이메일 입니다."),
    SIGNUP_VERIFICATION_REQUIRED("이메일 인증코드 확인이 필요합니다.");

    private final String message;

    SignupFailReason(String message) {
        this.message = message;
    }
}
