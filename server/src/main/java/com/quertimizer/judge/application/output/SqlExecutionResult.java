package com.quertimizer.judge.application.output;

import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Getter
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
        this.mode = Objects.requireNonNull(mode, "필수 값이 없다.");
        this.columns = List.copyOf(Objects.requireNonNull(columns, "필수 값이 없다."));
        this.rows = copyRows(rows);
        this.rowCount = rowCount;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.executionTimeMs = executionTimeMs;
        this.cost = cost;
        this.planLines = List.copyOf(Objects.requireNonNull(planLines, "필수 값이 없다."));
        this.message = message;
    }

    private List<List<String>> copyRows(List<List<String>> rows) {
        Objects.requireNonNull(rows, "필수 값이 없다.");

        return rows.stream()
                .map(row -> List.copyOf(Objects.requireNonNull(row, "필수 값이 없다."))).
                toList();
    }
}
