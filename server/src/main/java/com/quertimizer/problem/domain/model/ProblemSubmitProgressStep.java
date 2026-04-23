package com.quertimizer.problem.domain.model;

public enum ProblemSubmitProgressStep {

    VALIDATE("validate"),
    ANSWER("answer"),
    DDL("ddl"),
    PLAN("plan");

    private final String key;

    ProblemSubmitProgressStep(String key) {
        this.key = key;
    }

    public String getKey() {
        return key;
    }

}
