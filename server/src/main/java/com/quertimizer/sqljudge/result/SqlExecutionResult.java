package com.quertimizer.sqljudge.result;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * Represents a SQL execution result without Quertimizer business terms.
 */
public class SqlExecutionResult {

    private final ExecutionMode mode;
    private final List<String> columns;
    private final List<List<String>> rows;
    private final long rowCount;
    private final int currentPage;
    private final int pageSize;
    private final Long executionTimeMs;
    private final BigDecimal cost;
    private final List<String> planLines;
    private final String message;

    /**
     * Creates a SQL execution result.
     *
     * @param mode execution mode
     * @param columns result column names
     * @param rows result rows
     * @param rowCount total row count
     * @param currentPage current result page
     * @param pageSize result page size
     * @param executionTimeMs execution time in milliseconds
     * @param cost estimated or measured cost
     * @param planLines execution plan lines
     * @param message result message
     */
    public SqlExecutionResult(ExecutionMode mode,
                              List<String> columns,
                              List<List<String>> rows,
                              long rowCount,
                              int currentPage,
                              int pageSize,
                              Long executionTimeMs,
                              BigDecimal cost,
                              List<String> planLines,
                              String message) {
        this.mode = Objects.requireNonNull(mode, "mode must not be null");
        this.columns = List.copyOf(Objects.requireNonNull(columns, "columns must not be null"));
        this.rows = copyRows(rows);
        this.rowCount = rowCount;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.executionTimeMs = executionTimeMs;
        this.cost = cost;
        this.planLines = List.copyOf(Objects.requireNonNull(planLines, "planLines must not be null"));
        this.message = message;
    }

    /**
     * Returns the execution mode.
     *
     * @return execution mode
     */
    public ExecutionMode getMode() {
        return mode;
    }

    /**
     * Returns the result column names.
     *
     * @return result column names
     */
    public List<String> getColumns() {
        return columns;
    }

    /**
     * Returns the result rows.
     *
     * @return result rows
     */
    public List<List<String>> getRows() {
        return rows;
    }

    /**
     * Returns the total row count.
     *
     * @return total row count
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

    /**
     * Returns the execution time in milliseconds.
     *
     * @return execution time in milliseconds
     */
    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    /**
     * Returns the estimated or measured cost.
     *
     * @return estimated or measured cost
     */
    public BigDecimal getCost() {
        return cost;
    }

    /**
     * Returns the execution plan lines.
     *
     * @return execution plan lines
     */
    public List<String> getPlanLines() {
        return planLines;
    }

    /**
     * Returns the result message.
     *
     * @return result message
     */
    public String getMessage() {
        return message;
    }

    private List<List<String>> copyRows(List<List<String>> rows) {
        Objects.requireNonNull(rows, "rows must not be null");

        return rows.stream()
                .map(row -> List.copyOf(Objects.requireNonNull(row, "row must not be null"))).
                toList();
    }
}
