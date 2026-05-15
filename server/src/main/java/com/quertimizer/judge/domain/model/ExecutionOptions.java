package com.quertimizer.judge.domain.model;

import lombok.Data;

import static com.quertimizer.judge.domain.model.SqlPolicyFailReason.POSITIVE_VALUE_REQUIRED;

@Data
public class ExecutionOptions {

    public static final int DEFAULT_TIMEOUT_SECONDS = 180;

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
            throw new IllegalArgumentException(POSITIVE_VALUE_REQUIRED.getMessage());
        }

        if (page <= 0) {
            throw new IllegalArgumentException(POSITIVE_VALUE_REQUIRED.getMessage());
        }

        if (pageSize <= 0) {
            throw new IllegalArgumentException(POSITIVE_VALUE_REQUIRED.getMessage());
        }

        this.timeoutSeconds = timeoutSeconds;
        this.page = page;
        this.pageSize = pageSize;
        this.includeCost = includeCost;
        this.includePlan = includePlan;
        this.validateSql = validateSql;
    }

    public static ExecutionOptions interactive() {
        return new ExecutionOptions(DEFAULT_TIMEOUT_SECONDS, 1, 10, true, false);
    }

    public static ExecutionOptions officialCost() {
        return new ExecutionOptions(DEFAULT_TIMEOUT_SECONDS, 1, 100, true, true);
    }

    public static ExecutionOptions submissionAnswer() {
        return new ExecutionOptions(DEFAULT_TIMEOUT_SECONDS, 1, 10_000, true, false);
    }

    public static ExecutionOptions internalMetadata(int pageSize) {
        return new ExecutionOptions(DEFAULT_TIMEOUT_SECONDS, 1, pageSize, false, false, false);
    }
}
