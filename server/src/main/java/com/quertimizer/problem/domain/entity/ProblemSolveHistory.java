package com.quertimizer.problem.domain.entity;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ProblemSolveHistory {

    private String problemId;
    private String handle;
    private DbmsType dbmsType;
    private String submittedSql;
    private long executionTimeMs;
    private double cost;
    private long scanRows;
    private long executionPlanElement;
    private LocalDateTime submittedAt;

    public static ProblemSolveHistory create(String problemId,
                                             String handle,
                                             DbmsType dbmsType,
                                             String submittedSql,
                                             long executionTimeMs,
                                             double cost,
                                             long scanRows,
                                             long executionPlanElement,
                                             LocalDateTime submittedAt) {
        return new ProblemSolveHistory(
                problemId,
                handle,
                dbmsType,
                submittedSql,
                executionTimeMs,
                cost,
                scanRows,
                executionPlanElement,
                submittedAt
        );
    }

    public static ProblemSolveHistory restore(String problemId, String handle,
                                              DbmsType dbmsType, String submittedSql,
                                              long executionTimeMs, double cost,
                                              long scanRows, long executionPlanElement,
                                              LocalDateTime submittedAt) {
        // 저장된 문제 최고 기록 상태 복원
        return new ProblemSolveHistory(
                problemId, handle, dbmsType, submittedSql,
                executionTimeMs, cost, scanRows, executionPlanElement, submittedAt
        );
    }

    private ProblemSolveHistory(String problemId,
                                String handle,
                                DbmsType dbmsType,
                                String submittedSql,
                                long executionTimeMs,
                                double cost,
                                long scanRows,
                                long executionPlanElement,
                                LocalDateTime submittedAt) {
        this.problemId = problemId;
        this.handle = handle;
        this.dbmsType = dbmsType;
        this.submittedSql = submittedSql;
        this.executionTimeMs = executionTimeMs;
        this.cost = cost;
        this.scanRows = scanRows;
        this.executionPlanElement = executionPlanElement;
        this.submittedAt = submittedAt;
    }

}
