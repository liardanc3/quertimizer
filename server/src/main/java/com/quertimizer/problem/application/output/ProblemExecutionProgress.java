package com.quertimizer.problem.application.output;

import lombok.Data;

@Data
public class ProblemExecutionProgress {

    private final String problemId;
    private final String message;

    public static ProblemExecutionProgress waiting(String problemId, int remainingTasks) {
        return new ProblemExecutionProgress(problemId, "대기중 - " + remainingTasks);
    }
}
