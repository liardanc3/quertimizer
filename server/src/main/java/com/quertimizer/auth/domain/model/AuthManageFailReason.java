package com.quertimizer.auth.domain.model;

import lombok.Getter;

@Getter
public enum AuthManageFailReason {

    USER_NOT_FOUND("존재하지 않는 사용자입니다."),
    INVALID_ROLE("지원하지 않는 역할입니다."),
    SENSITIVE_CONFIRMATION_REQUIRED("민감 작업 확인 값이 올바르지 않습니다."),
    SELF_ADMIN_REMOVAL_DENIED("자기 자신의 Admin 권한은 해제할 수 없습니다."),
    LAST_ADMIN_PROTECTION("마지막 Admin 역할은 해제할 수 없습니다.");

    private final String message;

    AuthManageFailReason(String message) {
        this.message = message;
    }
}
