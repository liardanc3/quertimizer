package com.quertimizer.auth.domain.model;

import lombok.Getter;

@Getter
public enum AuthFailReason {

    USER_NOT_FOUND("존재하지 않는 사용자입니다."),
    HANDLE_ALREADY_CONFIGURED("이미 Handle 설정이 완료되었습니다."),
    OAUTH2_AUTHENTICATION_NOT_FOUND("인증 정보가 없습니다."),
    LOGIN_INFORMATION_NOT_FOUND("로그인 정보가 없습니다."),
    MAIL_CREATION_FAILED("메일 생성에 실패했습니다."),
    VERIFICATION_FAILURE_LIMIT_EXCEEDED("인증코드 실패 횟수를 초과했습니다. 인증코드를 다시 요청해 주세요."),
    BLOCKED_IP("차단된 IP입니다."),
    LOGIN_RATE_LIMIT_EXCEEDED("로그인 실패 횟수가 많습니다. 잠시 후 다시 시도해 주세요."),
    CODE_ISSUE_RATE_LIMIT_EXCEEDED("인증코드 요청이 많습니다. 잠시 후 다시 시도해 주세요."),
    PASSWORD_RESET_RATE_LIMIT_EXCEEDED("비밀번호 재설정 요청이 많습니다. 잠시 후 다시 시도해 주세요.");

    private final String message;

    AuthFailReason(String message) {
        this.message = message;
    }
}
