package com.quertimizer.user.domain.model;

public enum UserAnomalyAction {

    SUBMIT("제출");

    private final String label;

    UserAnomalyAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        // 라벨 조회
        return label;
    }

}
