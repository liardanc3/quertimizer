package com.quertimizer.sqljudge.policy;

/**
 * Represents SQL execution options.
 */
public class ExecutionOptions {

    private final int timeoutSeconds;
    private final int page;
    private final int pageSize;
    private final boolean includeCost;
    private final boolean includePlan;

    /**
     * Creates SQL execution options.
     *
     * @param timeoutSeconds execution timeout in seconds
     * @param page requested result page
     * @param pageSize requested result page size
     * @param includeCost whether cost should be included
     * @param includePlan whether plan lines should be included
     */
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

    /**
     * Creates options for interactive SQL execution.
     *
     * @return interactive execution options
     */
    public static ExecutionOptions interactive() {
        return new ExecutionOptions(60, 1, 10, true, false);
    }

    /**
     * Creates options for official cost measurement.
     *
     * @return official cost measurement options
     */
    public static ExecutionOptions officialCost() {
        return new ExecutionOptions(60, 1, 100, true, true);
    }

    /**
     * Returns the execution timeout in seconds.
     *
     * @return execution timeout in seconds
     */
    public int getTimeoutSeconds() {
        return timeoutSeconds;
    }

    /**
     * Returns the requested result page.
     *
     * @return requested result page
     */
    public int getPage() {
        return page;
    }

    /**
     * Returns the requested result page size.
     *
     * @return requested result page size
     */
    public int getPageSize() {
        return pageSize;
    }

    /**
     * Returns whether cost should be included.
     *
     * @return true when cost should be included
     */
    public boolean isIncludeCost() {
        return includeCost;
    }

    /**
     * Returns whether plan lines should be included.
     *
     * @return true when plan lines should be included
     */
    public boolean isIncludePlan() {
        return includePlan;
    }
}
