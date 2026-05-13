package com.quertimizer.problem.domain.model;

public enum ProblemCreateProgressStatus {

    RUNNING("running"),
    SUCCESS("success"),
    ERROR("error");

    private final String value;

    ProblemCreateProgressStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        // 진행 상태 값 조회
        return value;
    }

}
