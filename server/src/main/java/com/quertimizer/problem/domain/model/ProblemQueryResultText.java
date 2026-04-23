package com.quertimizer.problem.domain.model;

public enum ProblemQueryResultText {

    SELECT_RESULT_RETURNED("조회 결과를 반환했다."),
    PLAN_RESULT_RETURNED("실행 계획을 반환했다."),
    WORKSPACE_CLEANUP_REQUESTED("작업용 스키마 정리를 요청했다."),
    COMMAND_EXECUTED("명령을 실행했다."),
    PROBLEM_EXECUTION_FAILED("문제 실행 처리에 실패했다.");

    private final String text;

    ProblemQueryResultText(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

}
