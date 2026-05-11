package com.quertimizer.judge.domain.model;

public class ExecutionOptions {

    private final int timeoutSeconds;
    private final int page;
    private final int pageSize;
    private final boolean includeCost;
    private final boolean includePlan;
    private final boolean validateSql;

    public ExecutionOptions(int timeoutSeconds, int page, int pageSize, boolean includeCost, boolean includePlan) {
        this(timeoutSeconds, page, pageSize, includeCost, includePlan, true);
    }

    public ExecutionOptions(int timeoutSeconds, int page, int pageSize,
                            boolean includeCost, boolean includePlan,
                            boolean validateSql) {
        if (timeoutSeconds <= 0) {
            throw new IllegalArgumentException("값은 0보다 커야 합니다.");
        }

        if (page <= 0) {
            throw new IllegalArgumentException("값은 0보다 커야 합니다.");
        }

        if (pageSize <= 0) {
            throw new IllegalArgumentException("값은 0보다 커야 합니다.");
        }

        this.timeoutSeconds = timeoutSeconds;
        this.page = page;
        this.pageSize = pageSize;
        this.includeCost = includeCost;
        this.includePlan = includePlan;
        this.validateSql = validateSql;
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

    public static ExecutionOptions internalMetadata(int pageSize) {
        return new ExecutionOptions(60, 1, pageSize, false, false, false);
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

    public boolean isValidateSql() {
        return validateSql;
    }
}
