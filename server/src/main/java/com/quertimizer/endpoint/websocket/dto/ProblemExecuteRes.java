package com.quertimizer.endpoint.websocket.dto;

import java.util.List;

public record ProblemExecuteRes(String type,
                                boolean success,
                                String problemId,
                                String mode,
                                String message,
                                List<String> columns,
                                List<List<String>> rows,
                                List<String> planLines,
                                long rowCount,
                                Long executionTimeMs) {

    public static ProblemExecuteRes connected(String userId) {
        return new ProblemExecuteRes(
                "connected",
                true,
                null,
                null,
                userId,
                List.of(),
                List.of(),
                List.of(),
                0,
                null
        );
    }

    public static ProblemExecuteRes executionSuccess(String problemId,
                                                     String mode,
                                                     String message,
                                                     List<String> columns,
                                                     List<List<String>> rows,
                                                     List<String> planLines,
                                                     long rowCount,
                                                     long executionTimeMs) {
        return new ProblemExecuteRes(
                "problem.execute.result",
                true,
                problemId,
                mode,
                message,
                columns,
                rows,
                planLines,
                rowCount,
                executionTimeMs
        );
    }

    public static ProblemExecuteRes executionFailure(String problemId, String message) {
        return new ProblemExecuteRes(
                "problem.execute.result",
                false,
                problemId,
                null,
                message,
                List.of(),
                List.of(),
                List.of(),
                0,
                null
        );
    }

    public static ProblemExecuteRes submitSuccess(String problemId, String message, Long executionTimeMs) {
        return new ProblemExecuteRes(
                "problem.submit.result",
                true,
                problemId,
                null,
                message,
                List.of(),
                List.of(),
                List.of(),
                0,
                executionTimeMs
        );
    }

    public static ProblemExecuteRes submitFailure(String problemId, String message) {
        return new ProblemExecuteRes(
                "problem.submit.result",
                false,
                problemId,
                null,
                message,
                List.of(),
                List.of(),
                List.of(),
                0,
                null
        );
    }

    public static ProblemExecuteRes leaveSuccess(String problemId) {
        return new ProblemExecuteRes(
                "problem.leave.result",
                true,
                problemId,
                null,
                "작업용 스키마 정리를 요청했다.",
                List.of(),
                List.of(),
                List.of(),
                0,
                null
        );
    }

    public static ProblemExecuteRes error(String message) {
        return new ProblemExecuteRes(
                "error",
                false,
                null,
                null,
                message,
                List.of(),
                List.of(),
                List.of(),
                0,
                null
        );
    }
}
