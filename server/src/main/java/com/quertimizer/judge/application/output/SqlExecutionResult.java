package com.quertimizer.judge.application.output;

import com.quertimizer.judge.domain.model.ExecutionMode;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
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

    public SqlExecutionResult(ExecutionMode mode, List<String> columns, List<List<String>> rows,
                              long rowCount, int currentPage, int pageSize,
                              Long executionTimeMs, BigDecimal cost,
                              List<String> planLines, String message) {
        this.mode = mode;
        this.columns = List.copyOf(columns);
        this.rows = copyRows(rows);
        this.rowCount = rowCount;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.executionTimeMs = executionTimeMs;
        this.cost = cost;
        this.planLines = List.copyOf(planLines);
        this.message = message;
    }

    private List<List<String>> copyRows(List<List<String>> rows) {
        return rows.stream().map(List::copyOf).toList();
    }
}
