package com.quertimizer.auth.domain.model;

import lombok.Getter;

@Getter
public enum AuthFailReason {

    USER_NOT_FOUND("존재하지 않는 사용자입니다."),
    HANDLE_ALREADY_CONFIGURED("이미 Handle 설정이 완료되었습니다."),
    OAUTH2_AUTHENTICATION_NOT_FOUND("인증 정보가 없습니다."),
    LOGIN_INFORMATION_NOT_FOUND("로그인 정보가 없습니다."),
    MAIL_CREATION_FAILED("메일 생성에 실패했습니다.");

    private final String message;

    AuthFailReason(String message) {
        this.message = message;
    }
}
