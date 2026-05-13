package com.quertimizer.problem.application.output;

import com.quertimizer.problem.domain.model.ProblemJudgeExecutionMode;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProblemJudgeExecutionResult {

    private final ProblemJudgeExecutionMode mode;
    private final List<String> columns;
    private final List<List<String>> rows;
    private final long rowCount;
    private final int currentPage;
    private final int pageSize;
    private final Long executionTimeMs;
    private final BigDecimal cost;
    private final List<String> planLines;

    public ProblemJudgeExecutionResult(ProblemJudgeExecutionMode mode,
                                       List<String> columns, List<List<String>> rows,
                                       long rowCount, int currentPage, int pageSize,
                                       Long executionTimeMs, BigDecimal cost, List<String> planLines) {
        this.mode = mode;
        this.columns = List.copyOf(columns);
        this.rows = copyRows(rows);
        this.rowCount = rowCount;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.executionTimeMs = executionTimeMs;
        this.cost = cost;
        this.planLines = List.copyOf(planLines);
    }

    private List<List<String>> copyRows(List<List<String>> rows) {
        // 실행 결과 행 목록 불변 복사
        return rows.stream().map(List::copyOf).toList();
    }
}
