package com.quertimizer.problem.domain.entity;

import com.quertimizer.judge.domain.model.DbmsType;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ProblemSubmitHistory {

    private Long submitId;
    private String problemId;
    private String handle;
    private DbmsType dbmsType;
    private String submittedSql;
    private boolean success;
    private String message;
    private long executionTimeMs;
    private double cost;
    private long rowCount;
    private Long executionPlanElement;
    private LocalDateTime submittedAt;

    public static ProblemSubmitHistory create(String problemId, String handle, DbmsType dbmsType,
                                              String submittedSql, boolean success, String message,
                                              long executionTimeMs, double cost, long rowCount,
                                              long executionPlanElement,
                                              LocalDateTime submittedAt) {
        return new ProblemSubmitHistory(
                null, problemId, handle, dbmsType, submittedSql, success,
                message, executionTimeMs, cost, rowCount, executionPlanElement, submittedAt
        );
    }

    public static ProblemSubmitHistory restore(Long submitId, String problemId, String handle, DbmsType dbmsType,
                                               String submittedSql, boolean success, String message,
                                               long executionTimeMs, double cost, long rowCount,
                                               Long executionPlanElement, LocalDateTime submittedAt) {
        return new ProblemSubmitHistory(
                submitId, problemId, handle, dbmsType, submittedSql, success,
                message, executionTimeMs, cost, rowCount, executionPlanElement, submittedAt
        );
    }

    private ProblemSubmitHistory(Long submitId, String problemId, String handle, DbmsType dbmsType,
                                 String submittedSql, boolean success, String message,
                                 long executionTimeMs, double cost, long rowCount,
                                 Long executionPlanElement, LocalDateTime submittedAt) {
        this.submitId = submitId;
        this.problemId = problemId;
        this.handle = handle;
        this.dbmsType = dbmsType;
        this.submittedSql = submittedSql;
        this.success = success;
        this.message = message;
        this.executionTimeMs = executionTimeMs;
        this.cost = cost;
        this.rowCount = rowCount;
        this.executionPlanElement = executionPlanElement;
        this.submittedAt = submittedAt;
    }

}
