package com.quertimizer.admin.domain.model;

public enum AdminAnomalyRangeBoundary {

    START("시작"),
    END("종료");

    private final String label;

    AdminAnomalyRangeBoundary(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

}
