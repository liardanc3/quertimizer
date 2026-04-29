package com.quertimizer.auth.domain.model;

import lombok.Getter;

@Getter
public enum AuthManageFailReason {

    USER_NOT_FOUND("존재하지 않는 사용자다."),
    INVALID_ROLE("지원하지 않는 역할이다."),
    SENSITIVE_CONFIRMATION_REQUIRED("민감 작업 확인 값이 올바르지 않다."),
    SELF_ADMIN_REMOVAL_DENIED("자기 자신의 Admin 권한은 해제할 수 없다."),
    LAST_ADMIN_PROTECTION("마지막 Admin 역할은 해제할 수 없다."),
    PROBLEM_GENERATOR_REQUIRED("ProblemGenerator만 문제 권한을 수정할 수 있다."),
    INVALID_PERMISSION_KEY("존재하지 않는 문제 또는 테이블셋 권한이 포함되어 있다.");

    private final String message;

    AuthManageFailReason(String message) {
        this.message = message;
    }
}
