package com.quertimizer.problem.application.output;

import com.quertimizer.problem.domain.model.ProblemJudgeExecutionMode;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Getter
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
        this.mode = Objects.requireNonNull(mode, "필수 값이 없다.");
        this.columns = List.copyOf(Objects.requireNonNull(columns, "필수 값이 없다."));
        this.rows = copyRows(rows);
        this.rowCount = rowCount;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.executionTimeMs = executionTimeMs;
        this.cost = cost;
        this.planLines = List.copyOf(Objects.requireNonNull(planLines, "필수 값이 없다."));
    }

    private List<List<String>> copyRows(List<List<String>> rows) {
        // 실행 결과 행 목록 불변 복사
        return Objects.requireNonNull(rows, "필수 값이 없다.").stream()
                .map(row -> List.copyOf(Objects.requireNonNull(row, "필수 값이 없다.")))
                .toList();
    }
}
