package com.quertimizer.admin.domain.model;

public enum AdminAnomalyAction {

    SUBMIT("제출");

    private final String label;

    AdminAnomalyAction(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

}
