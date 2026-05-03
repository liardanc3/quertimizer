package com.quertimizer.problem.domain.model;

public enum ProblemManagementFailReason {

    PROBLEM_SET_NOT_FOUND("존재하지 않는 테이블셋이다."),
    PROBLEM_NOT_FOUND("존재하지 않는 문제 번호다."),
    PROBLEM_NOT_IN_PROBLEM_SET("선택한 문제 번호가 현재 테이블셋에 속하지 않는다."),
    EXISTING_PROBLEM_ID_REQUIRED("기존 문제 번호가 필요하다."),
    PROBLEM_SET_ID_REQUIRED("테이블셋 번호가 필요하다."),
    DBMS_REQUIRED("DBMS 정보가 필요하다."),
    PROBLEM_CREATE_FAILED("문제 생성 실패"),
    PROBLEM_UPDATE_DATA_INVALID("문제 업데이트를 위한 데이터가 유효하지 않습니다.");

    private final String message;

    ProblemManagementFailReason(String message) {
        this.message = message;
    }

    public String getMessage() {
        // 실패 메시지 반환
        return message;
    }

}
