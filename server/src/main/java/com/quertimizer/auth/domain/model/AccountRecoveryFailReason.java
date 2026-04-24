package com.quertimizer.auth.domain.model;

import lombok.Getter;

@Getter
public enum AccountRecoveryFailReason {

    EMAIL_NOT_FOUND("등록되지 않은 이메일입니다."),
    INVALID_VERIFICATION_CODE("인증코드가 올바르지 않습니다."),
    EXPIRED_VERIFICATION_CODE("인증코드 유효시간이 만료되었습니다."),
    PASSWORD_RESET_VERIFICATION_REQUIRED("인증코드 확인이 필요합니다."),
    VERIFICATION_EMAIL_SEND_FAILED("인증코드 이메일 발송에 실패했습니다.");

    private final String message;

    AccountRecoveryFailReason(String message) {
        this.message = message;
    }
}
