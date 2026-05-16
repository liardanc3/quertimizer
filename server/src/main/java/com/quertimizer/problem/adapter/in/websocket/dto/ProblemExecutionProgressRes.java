package com.quertimizer.problem.adapter.in.websocket.dto;

import com.quertimizer.problem.application.output.ProblemExecutionProgress;
import lombok.Data;

@Data
public class ProblemExecutionProgressRes {

    private final String type;
    private final String problemId;
    private final String status;
    private final String message;

    public static ProblemExecutionProgressRes from(ProblemExecutionProgress progress) {
        // 문제 실행 progress를 WebSocket 응답 DTO로 변환
        return new ProblemExecutionProgressRes(
                "problem.execute.progress",
                progress.getProblemId(), progress.getStatus(), progress.getMessage()
        );
    }
}
