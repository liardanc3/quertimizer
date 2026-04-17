package com.quertimizer.endpoint.websocket.dto;

import java.util.List;

public record ProblemSubmitProgressRes(String type,
                                       String problemId,
                                       String stepKey,
                                       String status,
                                       String message,
                                       List<String> detailLines) {

    public static ProblemSubmitProgressRes of(String problemId,
                                              String stepKey,
                                              String status,
                                              String message,
                                              List<String> detailLines) {
        return new ProblemSubmitProgressRes(
                "problem.submit.progress",
                problemId,
                stepKey,
                status,
                message,
                detailLines != null ? detailLines : List.of()
        );
    }
}
