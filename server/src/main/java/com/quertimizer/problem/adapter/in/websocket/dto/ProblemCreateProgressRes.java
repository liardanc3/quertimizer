package com.quertimizer.problem.adapter.in.websocket.dto;

import com.quertimizer.problem.application.output.ProblemCreateProgress;
import lombok.Data;

@Data
public class ProblemCreateProgressRes {

    private final String type;
    private final String stepKey;
    private final String status;
    private final String message;
    private final Integer stepOrder;
    private final String problemId;

    public ProblemCreateProgressRes(String type, String stepKey,
                                    String status, String message,
                                    Integer stepOrder, String problemId) {
        this.type = type;
        this.stepKey = stepKey;
        this.status = status;
        this.message = message;
        this.stepOrder = stepOrder;
        this.problemId = problemId;
    }

    public static ProblemCreateProgressRes from(ProblemCreateProgress progress) {
        return new ProblemCreateProgressRes(
                "problem.create.progress",
                progress.getStepKey(), progress.getStatus(), progress.getMessage(), progress.getStepOrder(), null
        );
    }

    public static ProblemCreateProgressRes completed(String problemId) {
        return new ProblemCreateProgressRes(
                "problem.create.progress",
                "problem-create-complete", "success", "문제 생성 완료", Integer.MAX_VALUE, problemId
        );
    }

    public static ProblemCreateProgressRes failed(String message) {
        return new ProblemCreateProgressRes(
                "problem.create.progress",
                "problem-create-failed", "error", message, Integer.MAX_VALUE, null
        );
    }
}
