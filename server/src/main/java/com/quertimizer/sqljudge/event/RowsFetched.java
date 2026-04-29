package com.quertimizer.sqljudge.event;

import com.quertimizer.sqljudge.id.JudgeExecutionId;

/**
 * Indicates that result rows have been fetched.
 */
public class RowsFetched extends AbstractSqlJudgeEvent {

    private final long rowCount;
    private final int currentPage;
    private final int pageSize;

    /**
     * Creates a rows fetched event.
     *
     * @param executionId execution task ID
     * @param rowCount fetched row count
     * @param currentPage current result page
     * @param pageSize result page size
     */
    public RowsFetched(JudgeExecutionId executionId, long rowCount, int currentPage, int pageSize) {
        super(executionId);
        this.rowCount = rowCount;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
    }

    /**
     * Returns the fetched row count.
     *
     * @return fetched row count
     */
    public long getRowCount() {
        return rowCount;
    }

    /**
     * Returns the current result page.
     *
     * @return current result page
     */
    public int getCurrentPage() {
        return currentPage;
    }

    /**
     * Returns the result page size.
     *
     * @return result page size
     */
    public int getPageSize() {
        return pageSize;
    }
}
