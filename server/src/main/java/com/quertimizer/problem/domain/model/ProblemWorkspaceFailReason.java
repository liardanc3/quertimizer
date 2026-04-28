package com.quertimizer.problem.domain.model;

public enum ProblemWorkspaceFailReason {

    PROBLEM_NOT_FOUND("존재하지 않는 문제다."),
    TABLE_REQUIRED("문제에서 사용할 테이블이 없다."),
    WORKSPACE_PREPARATION_FAILED("작업용 스키마를 준비하지 못했다.");

    private final String message;

    ProblemWorkspaceFailReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        // 실패 메시지를 반환한다
        return message;
    }

}
