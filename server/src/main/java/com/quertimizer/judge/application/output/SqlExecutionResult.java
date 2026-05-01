package com.quertimizer.judge.application.output;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

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

    public ExecutionMode getMode() {
        return mode;
    }

    public List<String> getColumns() {
        return columns;
    }

    public List<List<String>> getRows() {
        return rows;
    }

    public long getRowCount() {
        return rowCount;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public Long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public List<String> getPlanLines() {
        return planLines;
    }

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
