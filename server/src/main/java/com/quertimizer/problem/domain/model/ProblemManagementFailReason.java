package com.quertimizer.problem.domain.model;

public enum ProblemManagementFailReason {

    PROBLEM_MANAGEMENT_ACCESS_DENIED("문제 관리 접근 권한이 없다."),
    NEW_PROBLEM_SET_PERMISSION_REQUIRED("신규 테이블셋 생성 권한이 없다."),
    PROBLEM_SET_ACCESS_DENIED("선택한 테이블셋 접근 권한이 없다."),
    PROBLEM_ACCESS_DENIED("선택한 문제 접근 권한이 없다."),
    PROBLEM_SET_NOT_FOUND("존재하지 않는 테이블셋이다."),
    PROBLEM_NOT_FOUND("존재하지 않는 문제 번호다."),
    PROBLEM_NOT_IN_PROBLEM_SET("선택한 문제 번호가 현재 테이블셋에 속하지 않는다."),
    EXISTING_PROBLEM_ID_REQUIRED("기존 문제 번호가 필요하다."),
    PROBLEM_SET_ID_REQUIRED("테이블셋 번호가 필요하다."),
    DBMS_REQUIRED("DBMS 정보가 필요하다.");

    private final String message;

    ProblemManagementFailReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        // 실패 메시지를 반환한다
        return message;
    }

}
