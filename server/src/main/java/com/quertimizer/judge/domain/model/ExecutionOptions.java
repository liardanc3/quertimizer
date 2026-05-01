package com.quertimizer.judge.domain.model;

public class ExecutionOptions {

    private final int timeoutSeconds;
    private final int page;
    private final int pageSize;
    private final boolean includeCost;
    private final boolean includePlan;

    public ExecutionOptions(int timeoutSeconds, int page, int pageSize, boolean includeCost, boolean includePlan) {
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("timeoutSeconds must be positive");
        }

        if (page <= 0) {
            throw new IllegalArgumentException("page must be positive");
        }

        if (pageSize <= 0) {
            throw new IllegalArgumentException("pageSize must be positive");
        }

        this.timeoutSeconds = timeoutSeconds;
        this.page = page;
        this.pageSize = pageSize;
        this.includeCost = includeCost;
        this.includePlan = includePlan;
    }

    public static ExecutionOptions interactive() {
        return new ExecutionOptions(60, 1, 10, true, false);
    }

    public static ExecutionOptions officialCost() {
        return new ExecutionOptions(60, 1, 100, true, true);
    }

    public static ExecutionOptions submissionAnswer() {
        return new ExecutionOptions(60, 1, 10_000, true, false);
    }

    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    public int getPage() {
        return page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public boolean isIncludeCost() {
        return includeCost;
    }

    public boolean isIncludePlan() {
        return includePlan;
    }
}
