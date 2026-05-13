package com.quertimizer.problem.adapter.in.websocket.dto;

import com.quertimizer.problem.application.output.ProblemExecutionOutput;
import com.quertimizer.problem.application.output.ProblemSubmissionOutput;
import com.quertimizer.problem.domain.model.ProblemQueryResultText;
import lombok.Data;

import java.util.List;

@Data
public class ProblemExecuteRes {

    private final String type;
    private final boolean success;
    private final String problemId;
    private final String mode;
    private final String message;
    private final List<String> columns;
    private final List<List<String>> rows;
    private final List<String> planLines;
    private final long rowCount;
    private final Integer currentPage;
    private final Integer pageSize;
    private final Long executionTimeMs;
    private final Double cost;
    private final List<String> reasons;

    public ProblemExecuteRes(String type, boolean success, String problemId, String mode, String message,
                             List<String> columns, List<List<String>> rows, List<String> planLines,
                             long rowCount, Integer currentPage, Integer pageSize,
                             Long executionTimeMs, Double cost, List<String> reasons) {
        this.type = type;
        this.success = success;
        this.problemId = problemId;
        this.mode = mode;
        this.message = message;
        this.columns = columns;
        this.rows = rows;
        this.planLines = planLines;
        this.rowCount = rowCount;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.executionTimeMs = executionTimeMs;
        this.cost = cost;
        this.reasons = reasons != null ? List.copyOf(reasons) : List.of();
    }

    public static ProblemExecuteRes connected(String handle) {
        return new ProblemExecuteRes(
                "connected", true, null, null, handle,
                List.of(), List.of(), List.of(),
                0, null, null, null, null, List.of()
        );
    }

    public static ProblemExecuteRes executionSuccess(String problemId,
                                                     String mode,
                                                     String message,
                                                     List<String> columns,
                                                     List<List<String>> rows,
                                                     List<String> planLines,
                                                     long rowCount,
                                                     Integer currentPage,
                                                     Integer pageSize,
                                                     long executionTimeMs,
                                                     Double cost) {
        return new ProblemExecuteRes(
                "problem.execute.result", true, problemId, mode, message,
                columns, rows, planLines,
                rowCount, currentPage, pageSize, executionTimeMs, cost, List.of()
        );
    }

    public static ProblemExecuteRes executionSuccess(ProblemExecutionOutput output) {
        return executionSuccess(
                output.getProblemId(), output.getMode(), output.getMessage(),
                output.getColumns(), output.getRows(), output.getPlanLines(),
                output.getRowCount(), output.getCurrentPage(), output.getPageSize(),
                output.getExecutionTimeMs(), output.getCost()
        );
    }

    public static ProblemExecuteRes executionFailure(String problemId, String message) {
        return executionFailure(problemId, message, List.of());
    }

    public static ProblemExecuteRes executionFailure(String problemId, String message, List<String> reasons) {
        return new ProblemExecuteRes(
                "problem.execute.result", false, problemId, null, message,
                List.of(), List.of(), List.of(),
                0, null, null, null, null, reasons
        );
    }

    public static ProblemExecuteRes submitSuccess(String problemId, String message, Long executionTimeMs) {
        return new ProblemExecuteRes(
                "problem.submit.result", true, problemId, null, message,
                List.of(), List.of(), List.of(),
                0, null, null, executionTimeMs, null, List.of()
        );
    }

    public static ProblemExecuteRes submitFailure(String problemId, String message) {
        return submitFailure(problemId, message, List.of());
    }

    public static ProblemExecuteRes submitFailure(String problemId, String message, List<String> reasons) {
        return new ProblemExecuteRes(
                "problem.submit.result", false, problemId, null, message,
                List.of(), List.of(), List.of(),
                0, null, null, null, null, reasons
        );
    }

    public static ProblemExecuteRes submitResult(ProblemSubmissionOutput output) {
        return output.isSuccess()
                ? submitSuccess(output.getProblemId(), output.getMessage(), output.getExecutionTimeMs())
                : submitFailure(output.getProblemId(), output.getMessage());
    }

    public static ProblemExecuteRes leaveSuccess(String problemId) {
        return new ProblemExecuteRes(
                "problem.leave.result", true, problemId, null, ProblemQueryResultText.WORKSPACE_CLEANUP_REQUESTED.getText(),
                List.of(), List.of(), List.of(),
                0, null, null, null, null, List.of()
        );
    }

    public static ProblemExecuteRes error(String message) {
        return error(message, List.of());
    }

    public static ProblemExecuteRes error(String message, List<String> reasons) {
        return new ProblemExecuteRes(
                "error", false, null, null, message,
                List.of(), List.of(), List.of(),
                0, null, null, null, null, reasons
        );
    }
}
