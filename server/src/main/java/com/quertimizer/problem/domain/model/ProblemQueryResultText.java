package com.quertimizer.problem.domain.model;

import lombok.Getter;

@Getter
public enum ProblemQueryResultText {

    SELECT_RESULT_RETURNED("조회 결과를 반환"),
    PLAN_RESULT_RETURNED("실행 계획을 반환"),
    WORKSPACE_CLEANUP_REQUESTED("작업용 스키마 정리 요청"),
    COMMAND_EXECUTED("명령 실행."),
    PROBLEM_EXECUTION_FAILED("문제 실행 처리에 실패");

    // 문구 조회
    private final String text;

    ProblemQueryResultText(String text) {
        this.text = text;
    }

}
