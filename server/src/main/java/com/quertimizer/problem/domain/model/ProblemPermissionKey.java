package com.quertimizer.problem.domain.model;

public enum ProblemPermissionKey {

    NEW("NEW");

    private final String value;

    ProblemPermissionKey(String value) {
        this.value = value;
    }

    public String getValue() {
        // 값 조회
        return value;
    }

}
