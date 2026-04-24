package com.quertimizer.user.domain.model;

public enum UserAnomalyRangeBoundary {

    START("시작"),
    END("종료");

    private final String label;

    UserAnomalyRangeBoundary(String label) {
        this.label = label;
    }

    public String getLabel() {
        // 라벨 조회
        return label;
    }

}
