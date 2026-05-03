package com.quertimizer.dashboard.domain.model;

public final class DashboardHotPostConstant {

    public static final int DISPLAY_LIMIT = 11;
    public static final double LIKE_WEIGHT = 5d;
    public static final double COMMENT_WEIGHT = 4d;
    public static final double VIEW_WEIGHT = 0.35d;
    public static final double RECENCY_WEIGHT = 0.8d;
    public static final long RECENCY_BONUS_HOURS = 72L;

    private DashboardHotPostConstant() {
    }
}
