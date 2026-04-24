package com.quertimizer.problem.domain.model;

public enum ProblemLogMessage {

    RESIDUAL_WORKSPACE_CLEANUP_FAILED("잔여 작업용 스키마 정리에 실패했다."),
    WORKSPACE_CLEANUP_FAILED("작업용 스키마 정리에 실패했다. schema={}"),
    ALARM_SOCKET_SEND_FAILED("알람 WebSocket 전송에 실패했다."),
    EXECUTE_FAILURE_RESPONSE_SEND_FAILED("문제 실행 실패 응답 전송에 실패했다."),
    EXECUTE_PAGE_RESPONSE_SEND_FAILED("문제 실행 페이지 응답 전송에 실패했다."),
    SUBMIT_FAILURE_RESPONSE_SEND_FAILED("문제 제출 실패 응답 전송에 실패했다."),
    SUBMIT_PROGRESS_RESPONSE_SEND_FAILED("제출 진행 상태 응답 전송에 실패했다.");

    private final String message;

    ProblemLogMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        // 메시지 조회
        return message;
    }

}
