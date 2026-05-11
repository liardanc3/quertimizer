package com.quertimizer.problem.domain.model;

public enum ProblemLogMessage {

    RESIDUAL_WORKSPACE_CLEANUP_FAILED("잔여 작업용 스키마 정리 실패"),
    WORKSPACE_CLEANUP_FAILED("작업용 스키마 정리 실패 schema={}"),
    CREATE_PROGRESS_RESPONSE_SEND_FAILED("문제 생성 진행 상태 응답 전송 실패"),
    EXECUTE_FAILURE_RESPONSE_SEND_FAILED("문제 실행 실패 응답 전송 실패"),
    EXECUTE_PAGE_RESPONSE_SEND_FAILED("문제 실행 페이지 응답 전송 실패"),
    SUBMIT_FAILURE_RESPONSE_SEND_FAILED("문제 제출 실패 응답 전송 실패"),
    SUBMIT_PROGRESS_RESPONSE_SEND_FAILED("제출 진행 상태 응답 전송 실패");

    private final String message;

    ProblemLogMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        // 실패 메시지 반환
        return message;
    }

}
