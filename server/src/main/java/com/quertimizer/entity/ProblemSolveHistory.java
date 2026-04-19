package com.quertimizer.entity;

import com.quertimizer.constant.DbmsType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@IdClass(ProblemSolveHistoryId.class)
@Table(name = "problem_top_solve_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProblemSolveHistory {

    @Id
    @Column(name = "problem_id", nullable = false, length = 12)
    private String problemId;

    @Id
    @Column(name = "user_id", nullable = false, length = 50)
    private String userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dbms_type", length = 20)
    private DbmsType dbmsType;

    @Column(name = "submitted_sql", nullable = false, columnDefinition = "TEXT")
    private String submittedSql;

    @Column(name = "execution_time_ms", nullable = false)
    private long executionTimeMs;

    @Column(nullable = false)
    private double cost;

    @Column(name = "scan_rows", nullable = false)
    private long scanRows;

    @Column(name = "execution_plan_element", nullable = false)
    private long executionPlanElement;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    public static ProblemSolveHistory create(String problemId,
                                             String userId,
                                             DbmsType dbmsType,
                                             String submittedSql,
                                             long executionTimeMs,
                                             double cost,
                                             long scanRows,
                                             long executionPlanElement,
                                             LocalDateTime submittedAt) {
        return new ProblemSolveHistory(
                problemId,
                userId,
                dbmsType,
                submittedSql,
                executionTimeMs,
                cost,
                scanRows,
                executionPlanElement,
                submittedAt
        );
    }

    private ProblemSolveHistory(String problemId,
                                String userId,
                                DbmsType dbmsType,
                                String submittedSql,
                                long executionTimeMs,
                                double cost,
                                long scanRows,
                                long executionPlanElement,
                                LocalDateTime submittedAt) {
        this.problemId = problemId;
        this.userId = userId;
        this.dbmsType = dbmsType;
        this.submittedSql = submittedSql;
        this.executionTimeMs = executionTimeMs;
        this.cost = cost;
        this.scanRows = scanRows;
        this.executionPlanElement = executionPlanElement;
        this.submittedAt = submittedAt;
    }

}
