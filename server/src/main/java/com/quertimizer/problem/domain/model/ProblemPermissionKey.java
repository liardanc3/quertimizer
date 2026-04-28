package com.quertimizer.problem.domain.model;

public enum ProblemPermissionKey {

    NEW("NEW");

    private final String value;

    ProblemPermissionKey(String value) {
        this.value = value;
    }

    public String getValue() {
        // 저장 값을 반환한다
        return value;
    }

}
