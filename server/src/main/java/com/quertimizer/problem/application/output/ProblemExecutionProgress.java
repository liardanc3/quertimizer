package com.quertimizer.problem.application.output;

import lombok.Data;

@Data
public class ProblemExecutionProgress {

    private final String problemId;
    private final String status;
    private final String message;

    public static ProblemExecutionProgress waiting(String problemId, int remainingTasks) {
        return new ProblemExecutionProgress(problemId, "waiting", "SQL 실행 대기 중 - " + remainingTasks);
    }

    public static ProblemExecutionProgress running(String problemId) {
        return new ProblemExecutionProgress(problemId, "running", "SQL을 실행하는 중입니다.");
    }
}
